/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.digitalservicestax.connectors

import ltbs.uniform._
import interpreters.playframework._
import common.web.Codec
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.digitalservicestax.data.Return

import scala.concurrent.duration._
import java.time.LocalDateTime
import play.modules.reactivemongo._
import concurrent.{Future,ExecutionContext}
import play.api._, mvc.{Codec => _,_}
import uk.gov.hmrc.digitalservicestax.data.BackendAndFrontendJson._
import play.api.libs.json._
import reactivemongo.play.json._, collection._

object MongoPersistence {
  case class Wrapper(
    session: String,
    data: DB,
    ret: Option[Return] = None,
    timestamp: LocalDateTime = LocalDateTime.now
  )

  implicit val formatMap: OFormat[DB] = new OFormat[DB] {
    def writes(o: DB) = JsObject ( o.map {
      case (k,v) => (k.mkString("/"), JsString(v))
    }.toSeq )

    def reads(json: JsValue): JsResult[DB] = json match {
      case JsObject(data) => JsSuccess(data.map {
        case (k, JsString(v)) => (k.split("/").toList, v)
        case e => throw new IllegalArgumentException(s"cannot parse $e")
      }.toMap)
      case e => JsError(s"expected an object, got $e")
    }
  }  

}

case class MongoPersistence[A <: Request[AnyContent]] (
  reactiveMongoApi: ReactiveMongoApi, 
  collectionName: String,
  expireAfter: Duration,
  useMongoTTL: Boolean = false
)(getSession: A => String)(implicit ec: ExecutionContext) extends PersistenceEngine[A] {
  import MongoPersistence._
  import reactiveMongoApi.database

  def killDate: LocalDateTime = LocalDateTime.now.minusNanos(expireAfter.toNanos)

  def reaver: Future[Unit] = {
    val selector = Json.obj(
      "timestamp" -> Json.obj("$lt" -> killDate)
    )

    collection.map {
      _.delete.one(selector, Some(100))
    }
  }

  lazy val collection: Future[JSONCollection] = {
    database.map(_.collection[JSONCollection](collectionName)).flatMap { c =>

      import reactivemongo.api.indexes._

      val sessionIndex = Index(
        key = Seq("session" -> IndexType.Ascending),
        unique = true
      )

      val timestamp = Index(
        key = Seq("timestamp" -> IndexType.Ascending),
        unique = false //,
//        expire = Some(expireAfter.toSeconds.toInt)
      )

      val im = c.indexesManager

      if (useMongoTTL) { 
        Future.sequence(List(
          im.ensure(sessionIndex),
          im.ensure(timestamp)
        )).map(_ => c)
      } else {
        im.ensure(sessionIndex).map(_ => c)
      }
    }
  }

  implicit val wrapperFormat = Json.format[Wrapper]

  def selector(request: A): JsObject = Json.obj("session" -> getSession(request))

  def apply(request: A)(f: DB => Future[(DB, Result)]): Future[Result] = {
    collection.flatMap(_.find(selector(request)).one[Wrapper]).flatMap {
      case Some(Wrapper(_, data, _, d)) if {d isBefore killDate} & !useMongoTTL =>
        reaver.flatMap{_ => f(DB.empty)}
      case Some(Wrapper(_, data, _, _)) =>
        f(data)
      case None =>
        f(DB.empty)
    } flatMap { case (newDb, result) =>
        val wrapper = Wrapper(getSession(request), newDb)
        collection.flatMap(_.update(selector(request), wrapper, upsert = true).map{
          case wr: WriteResult if wr.writeErrors.isEmpty => result
          case e => throw new Exception(s"$e")
        })
    }
  }

  // N.b. this removes the entire record
  def getDirect[T](key: String*)(implicit request: A, codec: Codec[T]): Future[Either[ErrorTree, T]] = {
    val dbF: Future[DB] = collection.flatMap(_.findAndRemove(selector(request)).map(_.result[Wrapper])).map {
      case Some(Wrapper(_, data, _, _)) => data
      case None => DB.empty
    }
    dbF.map(_.get(key.toList).map {Input.fromUrlEncodedString(_) flatMap codec.decode} match {
      case None => Left(ltbs.uniform.ErrorMsg("not found").toTree)
      case Some(x) => x
    })
  }

  def cacheReturn(ret: Return, purgeStateUponCompletion: Boolean = false)(implicit request: A): Future[Unit] = {
    collection.flatMap(_.find(selector(request)).one[Wrapper]).map {
      case Some(wrapper) if purgeStateUponCompletion => wrapper.copy(ret = Some(ret), data = DB.empty)
      case Some(wrapper) => wrapper.copy(ret = Some(ret))
      case None => Wrapper(getSession(request), DB.empty, Some(ret))
    }.flatMap { wrapper =>
      collection.flatMap(_.update(ordered = false).one(selector(request), wrapper).map {
        case wr: reactivemongo.api.commands.WriteResult if wr.writeErrors.isEmpty =>
          (())
        case e => throw new Exception(s"$e")
      })
    }
  }

  def retrieveCachedReturn(implicit request: A): Future[Option[Return]] = {
    collection.flatMap(_.find(selector(request)).one[Wrapper]).map {
      case Some(wrapper) => wrapper.ret
      case _ => None
    }
  }
}
