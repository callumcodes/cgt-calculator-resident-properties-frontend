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

package views.resident.properties.report

import assets.MessageLookup.Resident.{Properties => propertiesMessages}
import assets.MessageLookup.{SummaryPage => messages}
import assets.{DateAsset, MessageLookup => commonMessages}
import common.Dates
import controllers.helpers.FakeRequestHelper
import models.resident._
import models.resident.income.{CurrentIncomeModel, PersonalAllowanceModel}
import models.resident.properties._
import org.jsoup.Jsoup
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import views.html.calculation.resident.properties.{report => views}

class PropertiesFinalReportViewSpec extends UnitSpec with WithFakeApplication with FakeRequestHelper {

  "Final Summary view" should {

    lazy val gainAnswers = YourAnswersSummaryModel(Dates.constructDate(10, 10, 2015),
      None,
      None,
      whoDidYouGiveItTo = Some("Other"),
      worthWhenGaveAway = Some(10000),
      BigDecimal(10000),
      None,
      worthWhenInherited = None,
      worthWhenGifted = None,
      worthWhenBoughtForLess = None,
      BigDecimal(10000),
      BigDecimal(30000),
      true,
      None,
      true,
      Some(BigDecimal(5000)),
      None,
      None
    )

    lazy val deductionAnswers = ChargeableGainAnswers(
      Some(LossesBroughtForwardModel(false)),
      None,
      Some(PropertyLivedInModel(false)),
      None,
      None,
      None,
      None
    )

    lazy val incomeAnswers = IncomeAnswersModel(Some(CurrentIncomeModel(0)), Some(PersonalAllowanceModel(0)))

    lazy val results = TotalGainAndTaxOwedModel(
      50000,
      20000,
      0,
      30000,
      3600,
      30000,
      18,
      Some(10000),
      Some(28),
      Some(BigDecimal(0)),
      Some(BigDecimal(0)),
      0,
      0
    )

    lazy val taxYearModel = TaxYearModel("2015/16", true, "2015/16")

    lazy val view = views.finalSummaryReport(gainAnswers, deductionAnswers, incomeAnswers, results, taxYearModel, false)(fakeRequestWithSession, applicationMessages)
    lazy val doc = Jsoup.parse(view.body)

    s"have a title ${messages.title}" in {
      doc.title() shouldBe messages.title
    }

    "have a page heading" which {
        //TODO: Update when merged in tax owed
//      s"includes a secondary heading with text '${messages.pageHeading}'" in {
//        doc.select("h1 span.pre-heading").text shouldBe messages.pageHeading
//      }
//
//      "includes an amount of tax due of £3,600.00" in {
//        doc.select("h1").text should include("£3,600.00")
//      }
    }

    "have the HMRC logo with the HMRC name" in {
      doc.select("span.organisation-logo-text").text shouldBe "HM Revenue & Customs"
    }

    "does not have a notice summary" in {
      doc.select("div.notice-wrapper").isEmpty shouldBe true
    }

    "have a section for the Calculation Details" which {

      "has the class 'summary-section' to underline the heading" in {
        doc.select("section#calcDetails h2").hasClass("summary-underline") shouldBe true
      }

      "has a h2 tag" which {

        s"should have the title '${messages.calcDetailsHeadingDate("2015/16")}'" in {
          doc.select("section#calcDetails h2").text shouldBe messages.calcDetailsHeadingDate("2015/16")
        }

        "has the class 'heading-large'" in {
          doc.select("section#calcDetails h2").hasClass("heading-large") shouldBe true
        }
      }

      "has a numeric output row for the gain" which {

        "should have the question text 'Total Gain'" in {
          doc.select("#gain-question").text shouldBe messages.totalGain
        }

        "should have the value '£50,000'" in {
          doc.select("#gain-amount").text shouldBe "£50,000"
        }
      }

      "has a numeric output row for the deductions" which {

        "should have the question text 'Deductions'" in {
          doc.select("#deductions-question").text shouldBe messages.deductions
        }

        "should have the value '£0'" in {
          doc.select("#deductions-amount").text should include("£0")
        }

        "has a breakdown that" should {

          "include a value for PRR of £0" in {
            doc.select("#deductions-amount").text should include("Private Residence Relief used £0")
          }

          "include a value for Reliefs of £0" in {
            doc.select("#deductions-amount").text should include(s"${messages.lettingReliefsUsed} £0")
          }

          "include a value for Capital gains tax allowance used of £0" in {
            doc.select("#deductions-amount").text should include(s"${messages.deductionsDetailsCapitalGainsTax} £0")
          }

          "include a value for Loss brought forward of £0" in {
            doc.select("#deductions-amount").text should include(s"${messages.deductionsDetailsLossBeforeYearUsed("2015/16")} £0")
          }
        }
      }

      "has a numeric output row for the chargeable gain" which {

        "should have the question text 'Taxable Gain'" in {
          doc.select("#chargeableGain-question").text shouldBe messages.chargeableGain
        }

        "should have the value '£20,000'" in {
          doc.select("#chargeableGain-amount").text should include("£20,000")
        }
      }

      "has a numeric output row and a tax rate" which {

        "should have the question text 'Tax Rate'" in {
          doc.select("#gainAndRate-question").text shouldBe messages.taxRate
        }

        "should have the value £30,000" in {
          doc.select("#firstBand").text should include("£30,000")
        }

        "should have the tax rate 18%" in {
          doc.select("#firstBand").text should include("18%")
        }

        "should have the value £10,000 in the second band" in {
          doc.select("#secondBand").text should include("£10,000")
        }

        "should have the tax rate 28% for the first band" in {
          doc.select("#secondBand").text should include("28%")
        }
      }

      "has a numeric output row for the AEA remaining" which {

        "should have the question text 'Capital Gains Tax allowance left for 2015/16" in {
          doc.select("#aeaRemaining-question").text should include(messages.aeaRemaining("2015/16"))
        }

        "include a value for Capital gains tax allowance left of £0" in {
          doc.select("#aeaRemaining-amount").text should include("£0")
        }
      }
    }

    s"have a section for Your answers" which {

      "has an entry for disposal date" in {
        doc.select("#disposalDate-question").size() shouldBe 1
      }

      "has an entry for brought forward losses" in {
        doc.select("#broughtForwardLosses-question").size() shouldBe 1
      }

      "has an entry for personal allowance" in {
        doc.select("#personalAllowance-question").size() shouldBe 1
      }
    }
  }

  "Final Summary when supplied with a date above the known tax years" should {

    lazy val taxYearModel = TaxYearModel(DateAsset.getYearAfterCurrentTaxYear, false, Dates.getCurrentTaxYear)

    lazy val gainAnswers = YourAnswersSummaryModel(Dates.constructDate(10, 10, 2018),
      None,
      Some(500),
      whoDidYouGiveItTo = None,
      worthWhenGaveAway = None,
      BigDecimal(10000),
      Some(BigDecimal(100000)),
      worthWhenInherited = None,
      worthWhenGifted = None,
      worthWhenBoughtForLess = None,
      BigDecimal(10000),
      BigDecimal(30000),
      false,
      Some(true),
      false,
      None,
      Some("Bought"),
      Some(false)
    )

    lazy val deductionAnswers = ChargeableGainAnswers(
      Some(LossesBroughtForwardModel(false)),
      None,
      Some(PropertyLivedInModel(true)),
      Some(PrivateResidenceReliefModel(true)),
      Some(PrivateResidenceReliefValueModel(5000)),
      Some(LettingsReliefModel(true)),
      Some(LettingsReliefValueModel(7000))
    )

    lazy val incomeAnswers = IncomeAnswersModel(Some(CurrentIncomeModel(0)), Some(PersonalAllowanceModel(0)))

    lazy val results = TotalGainAndTaxOwedModel(
      50000,
      20000,
      0,
      30000,
      3600,
      30000,
      18,
      Some(10000),
      Some(28),
      Some(BigDecimal(30000)),
      Some(BigDecimal(2000)),
      0,
      0
    )

    lazy val view = views.finalSummaryReport(gainAnswers, deductionAnswers, incomeAnswers, results, taxYearModel, false)(fakeRequestWithSession, applicationMessages)
    lazy val doc = Jsoup.parse(view.body)

    "has a numeric output row for the deductions" which {

      "should have the question text 'Deductions'" in {
        doc.select("#deductions-question").text shouldBe messages.deductions
      }

      "has a breakdown that" should {

        "include a value for PRR of £2,000" in {
          doc.select("#deductions-amount").text should include("Private Residence Relief used £2,000")
        }

        "include a value for Reliefs of £30,000" in {
          doc.select("#deductions-amount").text should include(s"${messages.lettingReliefsUsed} £30,000")
        }
      }
    }
  }
}
