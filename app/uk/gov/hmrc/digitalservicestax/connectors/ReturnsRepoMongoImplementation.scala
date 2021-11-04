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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Format
import play.api.mvc.Request

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import uk.gov.hmrc.digitalservicestax.config.AppConfig
import uk.gov.hmrc.digitalservicestax.data.BackendAndFrontendJson.returnFormat
import uk.gov.hmrc.digitalservicestax.data.Return
import uk.gov.hmrc.mongo.cache.{CacheIdType, EntityCache, MongoCacheRepository}
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}

@Singleton
class ReturnsRepoMongoImplementation @Inject()(
  mongoComponent: MongoComponent,
  config: AppConfig
)(
  implicit ec: ExecutionContext
) extends EntityCache[Request[Any],Return] with ReturnsRepo {

  lazy val cacheRepo: MongoCacheRepository[Request[Any]] = new MongoCacheRepository (
    mongoComponent = mongoComponent,
    collectionName = "return",
    ttl = config.mongoShortLivedStoreExpireAfter,
    timestampSupport = new CurrentTimestampSupport,
    cacheIdType = CacheIdType.SessionCacheId
  )

  lazy val format: Format[Return] = returnFormat
   
  def cacheReturn(ret: Return, purgeStateUponCompletion: Boolean)(implicit request: Request[Any]): Future[Unit] = putCache(request)(ret)
  def retrieveCachedReturn(implicit request: Request[Any]): Future[Option[Return]] =
    getFromCache(request)

}

