/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.resident.properties.DeductionsControllerSpec

import assets.MessageLookup.{LossesBroughtForwardValue => messages}
import common.KeystoreKeys.{ResidentPropertyKeys => keystoreKeys}
import config.AppConfig
import connectors.CalculatorConnector
import controllers.DeductionsController
import controllers.helpers.FakeRequestHelper
import models.resident._
import models.resident.properties.{ChargeableGainAnswers, YourAnswersSummaryModel}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class LossesBroughtForwardValueActionSpec extends UnitSpec with GuiceOneAppPerSuite with FakeRequestHelper with MockitoSugar{

  "Calling .lossesBroughtForwardValue from the resident DeductionsController" when {

    def setGetTarget(getData: Option[LossesBroughtForwardValueModel],
                     disposalDateModel: DisposalDateModel,
                     taxYearModel: TaxYearModel): DeductionsController = {

      val mockCalcConnector = mock[CalculatorConnector]

      when(mockCalcConnector.fetchAndGetFormData[LossesBroughtForwardValueModel](ArgumentMatchers.eq(keystoreKeys.lossesBroughtForwardValue))
        (ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(getData)

      when(mockCalcConnector.fetchAndGetFormData[DisposalDateModel](ArgumentMatchers.eq(keystoreKeys.disposalDate))
        (ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Some(disposalDateModel))

      when(mockCalcConnector.getTaxYear(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(taxYearModel)))

      new DeductionsController {
        override val calcConnector = mockCalcConnector
        val config = mock[AppConfig]
      }
    }

    "request has a valid session with no keystore data" should {
      lazy val disposalDateModel = DisposalDateModel(10, 10, 2015)
      lazy val taxYearModel = TaxYearModel("2015/16", true, "2015/16")
      lazy val target = setGetTarget(None, disposalDateModel, taxYearModel)
      lazy val result = target.lossesBroughtForwardValue(fakeRequestWithSession)

      "return a status of 200" in {
        status(result) shouldBe 200
      }

      s"return some html with " in {
        contentType(result) shouldBe Some("text/html")
      }

      s"return a title of ${messages.title("2015/16")}" in {
        Jsoup.parse(bodyOf(result)).title shouldEqual messages.title("2015/16")
      }

      s"have a back link to '${controllers.routes.DeductionsController.lossesBroughtForward().url}'" in {
        Jsoup.parse(bodyOf(result)).getElementById("back-link").attr("href") shouldEqual
          controllers.routes.DeductionsController.lossesBroughtForward().url
      }
    }

    "request has a valid session with some keystore data" should {
      lazy val disposalDateModel = DisposalDateModel(10, 10, 2014)
      lazy val taxYearModel = TaxYearModel("2014/15", false, "2015/16")
      lazy val target = setGetTarget(Some(LossesBroughtForwardValueModel(BigDecimal(1000))), disposalDateModel, taxYearModel)
      lazy val result = target.lossesBroughtForwardValue(fakeRequestWithSession)

      "return a status of 200" in {
        status(result) shouldBe 200
      }

      s"return some html with " in {
        contentType(result) shouldBe Some("text/html")
      }

      s"return a title of ${messages.title("2015/15")}" in {
        Jsoup.parse(bodyOf(result)).title shouldEqual messages.title("2014/15")
      }
    }

    "request has an invalid session" should {
      lazy val result = DeductionsController.lossesBroughtForwardValue(fakeRequest)

      "return a status of 303" in {
        status(result) shouldBe 303
      }

      "return you to the session timeout page" in {
        redirectLocation(result).get should include ("/calculate-your-capital-gains/resident/properties/session-timeout")
      }
    }
  }

  "Calling .submitLossesBroughtForwardValue from the resident DeductionsController" when {

    val gainModel = mock[YourAnswersSummaryModel]
    val summaryModel = mock[ChargeableGainAnswers]

    def setPostTarget(gainAnswers: YourAnswersSummaryModel,
                      chargeableGainAnswers: ChargeableGainAnswers,
                      chargeableGain: ChargeableGainResultModel,
                      disposalDateModel: DisposalDateModel,
                      taxYearModel: TaxYearModel): DeductionsController = {

      val mockCalcConnector = mock[CalculatorConnector]

      when(mockCalcConnector.getPropertyGainAnswers(ArgumentMatchers.any()))
        .thenReturn(Future.successful(gainAnswers))

      when(mockCalcConnector.getPropertyDeductionAnswers(ArgumentMatchers.any()))
        .thenReturn(Future.successful(chargeableGainAnswers))

      when(mockCalcConnector.calculateRttPropertyChargeableGain(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(chargeableGain)))

      when(mockCalcConnector.fetchAndGetFormData[DisposalDateModel](ArgumentMatchers.eq(keystoreKeys.disposalDate))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Some(disposalDateModel))

      when(mockCalcConnector.getTaxYear(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(taxYearModel)))

      new DeductionsController {
        override val calcConnector = mockCalcConnector
        val config = mock[AppConfig]
      }
    }

    "given a valid form" when {

      "the user has zero chargeable gain" should {
        lazy val disposalDateModel = DisposalDateModel(10, 10, 2015)
        lazy val taxYearModel = TaxYearModel("2015/16", true, "2015/16")
        lazy val target = setPostTarget(gainModel, summaryModel,
          ChargeableGainResultModel(2000, 0, 0, 0, 2000, BigDecimal(0), BigDecimal(0), Some(BigDecimal(0)),
            Some(BigDecimal(0)), 0, 0), disposalDateModel = disposalDateModel, taxYearModel = taxYearModel)
        lazy val request = fakeRequestToPOSTWithSession(("amount", "1000"))
        lazy val result = target.submitLossesBroughtForwardValue(request)

        "return a status of 303" in {
          status(result) shouldBe 303
        }

        s"redirect to '${controllers.routes.ReviewAnswersController.reviewDeductionsAnswers().url}'" in {
          redirectLocation(result).get shouldBe controllers.routes.ReviewAnswersController.reviewDeductionsAnswers().url
        }
      }

      "the user has negative chargeable gain" should {
        lazy val disposalDateModel = DisposalDateModel(10, 10, 2015)
        lazy val taxYearModel = TaxYearModel("2015/16", true, "2015/16")
        lazy val target = setPostTarget(gainModel, summaryModel,
          ChargeableGainResultModel(2000, -1000, 0, 0, 3000, BigDecimal(0), BigDecimal(0), Some(BigDecimal(0)),
            Some(BigDecimal(0)), 0, 0), disposalDateModel = disposalDateModel, taxYearModel = taxYearModel)
        lazy val request = fakeRequestToPOSTWithSession(("amount", "1000"))
        lazy val result = target.submitLossesBroughtForwardValue(request)

        "return a status of 303" in {
          status(result) shouldBe 303
        }

        s"redirect to '${controllers.routes.ReviewAnswersController.reviewDeductionsAnswers().url}'" in {
          redirectLocation(result).get shouldBe controllers.routes.ReviewAnswersController.reviewDeductionsAnswers().url
        }
      }

      "the user has positive chargeable gain of £1,000" should {
        lazy val disposalDateModel = DisposalDateModel(10, 10, 2015)
        lazy val taxYearModel = TaxYearModel("2015/16", true, "2015/16")
        lazy val target = setPostTarget(gainModel, summaryModel,
          ChargeableGainResultModel(1000, 1000, 0, 0, 0, BigDecimal(0), BigDecimal(0), Some(BigDecimal(0)),
            Some(BigDecimal(0)), 0, 0), disposalDateModel = disposalDateModel, taxYearModel = taxYearModel)
        lazy val request = fakeRequestToPOSTWithSession(("amount", "1000"))
        lazy val result = target.submitLossesBroughtForwardValue(request)

        "return a status of 303" in {
          status(result) shouldBe 303
        }

        s"redirect to '${controllers.routes.SummaryController.summary().toString}'" in {
          redirectLocation(result).get shouldBe controllers.routes.IncomeController.currentIncome().toString
        }
      }
    }

    "given an invalid form" should {
      lazy val disposalDateModel = DisposalDateModel(10, 10, 2015)
      lazy val taxYearModel = TaxYearModel("2015/16", true, "2015/16")
      lazy val target = setPostTarget(gainModel, summaryModel,
        ChargeableGainResultModel(1000, 1000, 0, 0, 0, BigDecimal(0), BigDecimal(0), Some(BigDecimal(0)),
          Some(BigDecimal(0)), 0, 0), disposalDateModel = disposalDateModel, taxYearModel = taxYearModel)
      lazy val request = fakeRequestToPOSTWithSession(("amount", ""))
      lazy val result = target.submitLossesBroughtForwardValue(request)

      "return a status of 400" in {
        status(result) shouldBe 400
      }

      s"return a title of ${messages.title("2015/16")}" in {
        Jsoup.parse(bodyOf(result)).title shouldEqual messages.title("2015/16")
      }
    }
  }
}
