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

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.http.Status
import play.api.mvc.{AnyContent, Results}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.digitalservicestax.data.{InternalId, Return}
import uk.gov.hmrc.digitalservicestax.util.FakeApplicationSetup
import uk.gov.hmrc.digitalservicestax.util.TestInstances._
import uk.gov.hmrc.digitalservicestaxfrontend.actions.AuthorisedRequest

import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MongoPersistenceSetup extends FakeApplicationSetup {

  val persistence = MongoPersistence[AuthorisedRequest[AnyContent]](
    reactiveMongoApi = reactiveMongoApi,
    collectionName = "test_persistence",
    expireAfter = appConfig.mongoShortLivedStoreExpireAfter,
    useMongoTTL = true
  ) { req =>
    req.internalId
  }

  "should generate a kill date based on a mongo TTL" in {
    info(persistence.killDate.format(DateTimeFormatter.ISO_DATE))
  }

  "should trigger reaver method to perform a delete" in {
    whenReady(persistence.reaver) { _ =>
      info("Reaver method invoked")
    }
  }

  "should generate a kill date and store it in Mongo for an authorised Request" in {
    forAll { (user: InternalId, enrolments: Enrolments) =>
      val req = AuthorisedRequest[AnyContent](user, enrolments, FakeRequest())

      val future = persistence(req) { db =>
        Future.successful(db -> Results.Ok("bla"))
      }

      whenReady(future) { res =>
        res.header.status mustBe Status.OK
      }
    }
  }

  "should cache and retrieve a Return" in {
    forAll { (user: InternalId, enrolments: Enrolments, ret: Return, purgeState: Boolean) =>
      implicit val req: AuthorisedRequest[AnyContent] = AuthorisedRequest[AnyContent](user, enrolments, FakeRequest())
      for {
        _ <- persistence.cacheReturn(ret, purgeState)
        retrieved <- persistence.retrieveCachedReturn
      } yield {
        retrieved.nonEmpty mustBe true
        retrieved.map(_ mustBe ret)
      }
    }
  }
}
