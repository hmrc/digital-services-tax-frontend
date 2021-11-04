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

import ltbs.uniform.interpreters.playframework.{DB, PersistenceEngine}
import play.api.libs.json._
import play.api.mvc.{Request, AnyContent, Result}
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, ExecutionContext}
import uk.gov.hmrc.mongo.cache.{CacheIdType, EntityCache, MongoCacheRepository}
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}

object MongoUniformPersistence {

  val formatMap: OFormat[DB] = new OFormat[DB] {
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

class MongoUniformPersistence[A <: Request[AnyContent]](
  collectionName: String,
  mongoComponent: MongoComponent,
  ttl: Duration
)(implicit ec: ExecutionContext) extends EntityCache[Request[Any], DB] with PersistenceEngine[A] {

  val cacheRepo: MongoCacheRepository[Request[Any]] = new MongoCacheRepository (
    mongoComponent = mongoComponent,
    collectionName = collectionName,
    ttl = ttl,
    timestampSupport = new CurrentTimestampSupport,
    cacheIdType = CacheIdType.SessionUuid("sessionId")
  )

  lazy val format: Format[DB] = MongoUniformPersistence.formatMap

  def apply(request: A)(f: DB => Future[(DB, Result)]): Future[Result] =
    getFromCache(request)
      .map(_.getOrElse(DB.empty))
      .flatMap(f)
      .flatMap{ case (newDb, result) => putCache(request)(newDb).map(_ => result)}
}
