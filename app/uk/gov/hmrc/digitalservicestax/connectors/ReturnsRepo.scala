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

import uk.gov.hmrc.digitalservicestax.data.Return
import scala.concurrent.Future
import play.api.mvc.Request
import com.google.inject.ImplementedBy

@ImplementedBy(classOf[ReturnsRepoMongoImplementation])
trait ReturnsRepo {
  def cacheReturn(ret: Return, purgeStateUponCompletion: Boolean = false)(implicit request: Request[Any]): Future[Unit]
  def retrieveCachedReturn(implicit request: Request[Any]): Future[Option[Return]]
}
