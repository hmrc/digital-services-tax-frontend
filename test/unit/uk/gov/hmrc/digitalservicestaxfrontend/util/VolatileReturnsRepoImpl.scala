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

package unit.uk.gov.hmrc.digitalservicestaxfrontend.util

import uk.gov.hmrc.digitalservicestax.connectors.ReturnsRepo
import uk.gov.hmrc.digitalservicestax.data.Return
import play.api.mvc.Request
import scala.concurrent.Future

object VolatileReturnsRepoImpl extends ReturnsRepo {

  private var state: Map[Request[Any],Return] = Map.empty[Request[Any], Return]

  def cacheReturn(ret: Return, purgeStateUponCompletion: Boolean = false)(implicit request: Request[Any]): Future[Unit] = {
    state = state + (request -> ret)
    Future.successful(())
  }

  def retrieveCachedReturn(implicit request: Request[Any]): Future[Option[Return]] =
    Future.successful(state.get(request))

}
