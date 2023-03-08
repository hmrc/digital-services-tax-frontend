/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.digitalservicestax.controllers

import cats.data.Validated
import cats.syntax.all._
import enumeratum.{Enum, EnumEntry}

import java.time.LocalDate
import javax.inject.Inject
import ltbs.uniform._, validation._, common.web._
import ltbs.uniform.interpreters.playframework.{PlayInterpreter, RichPlayMessages}
import org.jsoup.Jsoup
import play.api.Logger
import play.api.mvc.{AnyContent, Request}
import play.twirl.api.{Html, HtmlFormat}, HtmlFormat.Appendable
import shapeless.tag
import tag.@@
import uk.gov.hmrc.digitalservicestax.config.AppConfig
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.digitalservicestax.views.html.{FormWrapper, MainLayout}
import uk.gov.hmrc.digitalservicestax.views.html.uniform._
import uk.gov.hmrc.digitalservicestax.views.html.helpers.CountrySelect

class DSTInterpreter @Inject() (
  messagesApi: play.api.i18n.MessagesApi,
  layout: MainLayout,
  formWrapper: FormWrapper,
  standardField: StandardField,
  checkboxes: Checkboxes,
  stringField: StringField,
  radios: Radios,
  address: AddressField,
  countrySelect: CountrySelect,
  date: Date,
  listing: Listing
)(implicit
  val appConfig: AppConfig
) extends PlayInterpreter[Html]
    with InferWebAsk[Html]
    with Widgets {

  val logger = Logger(getClass)

  implicit def enumeratumField[A <: EnumEntry](implicit enums: Enum[A]): WebAsk[Html, A] =
    new WebAsk[Html, A] {

      def decode(out: Input): Either[ErrorTree, A] = out
        .toField[A](x =>
          Rule.nonEmpty[String].apply(x) andThen
            (y =>
              Validated
                .catchOnly[NoSuchElementException](enums.withName(y))
                .leftMap(_ => ErrorTree.oneErr(ErrorMsg("invalid")))
            )
        )
        .toEither

      def encode(in: A): Input = Input.one(List(in.entryName))
      def render(
        pageKey: List[String],
        fieldKey: List[String],
        tell: Option[Html],
        path: Breadcrumbs,
        data: Input,
        errors: ErrorTree,
        messages: UniformMessages[Html]
      ): Option[Html] = {
        val options       = enums.values.map(_.entryName)
        val existingValue = decode(data).map(_.entryName).toOption
        radios(
          fieldKey,
          options,
          existingValue,
          errors,
          messages
        ).some
      }
    }

  implicit lazy val twirlBoolField = new WebAsk[Html, Boolean] {
    val True  = true.toString.toUpperCase
    val False = false.toString.toUpperCase

    def decode(out: Input): Either[ErrorTree, Boolean] =
      out
        .toField[Boolean](x =>
          Rule.nonEmpty[String].apply(x) andThen (y =>
            Validated
              .catchOnly[IllegalArgumentException](y.toBoolean)
              .leftMap(_ => ErrorMsg("invalid").toTree)
          )
        )
        .toEither

    def encode(in: Boolean): Input = Input.one(List(in.toString))

    def render(
      pageKey: List[String],
      fieldKey: List[String],
      tell: Option[Html],
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Option[Html] = {
      val options       =
        if (pageKey.contains("about-you") || pageKey.contains("user-employed")) List(False, True) else List(True, False)
      val existingValue = data.toStringField().toOption
      radios(fieldKey, options, existingValue, errors, messages, tell = tell).some
    }
  }

  implicit val twirlStringField = twirlStringFields()

  implicit val twirlCountryCodeField = new WebAsk[Html, CountryCode] {

    override def render(
      pageKey: List[String],
      fieldKey: List[String],
      tell: Option[Html],
      breadcrumbs: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Option[Html] =
      countrySelect(
        fieldKey.mkString("."),
        data.values.flatten.headOption,
        errors.nonEmpty,
        messages
      ).some

    override def encode(in: CountryCode): Input =
      validatedVariant(CountryCode).encode(in)

    override def decode(out: Input): Either[ErrorTree, CountryCode] =
      validatedVariant(CountryCode).decode(out)

  }
  type CustomStringRenderer =
    (List[String], String, ErrorTree, UniformMessages[Html], Option[String]) => Appendable

  def twirlStringFields(
    autoFields: Option[String] = None,
    customRender: Option[CustomStringRenderer] = None
  ): WebAsk[Html, String] = new WebAsk[Html, String] {
    def decode(out: Input): Either[ErrorTree, String] =
      out.toStringField().toEither

    def encode(in: String): Input = Input.one(List(in))

    def render(
      pageKey: List[String],
      fieldKey: List[String],
      tell: Option[Html],
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Option[Html] = {
      val existingValue: String = data.valueAtRoot.flatMap(_.headOption).getOrElse("")

      customRender.fold(stringField.apply(fieldKey, existingValue, errors, messages, autoFields).some) { cr =>
        cr(fieldKey, existingValue, errors, messages, autoFields).some
      }

    }
  }

  implicit lazy val phoneField =
    validatedString(
      PhoneNumber,
      24
    )

  implicit def enumeratumSetField[A <: EnumEntry](implicit enums: Enum[A]): WebAsk[Html, Set[A]] =
    new WebAsk[Html, Set[A]] {

      def decode(out: Input): Either[ErrorTree, Set[A]] = {
        val i: List[String]               = out.valueAtRoot.getOrElse(Nil)
        val r: List[Either[ErrorTree, A]] = i.map { x =>
          Either
            .catchOnly[NoSuchElementException](enums.withName(x))
            .leftMap(_ => ErrorTree.oneErr(ErrorMsg("invalid")))
        }
        r.sequence.map(_.toSet)
      }

      // Members declared in ltbs.uniform.common.web.Codec
      def encode(in: Set[A]): Input =
        Map(Nil -> in.toList.map(_.entryName))

      // Members declared in ltbs.uniform.common.web.WebAsk
      def render(
        pageKey: List[String],
        fieldKey: List[String],
        tell: Option[Html],
        breadcrumbs: Breadcrumbs,
        data: Input,
        errors: ErrorTree,
        messages: UniformMessages[Html]
      ): Option[Html] = {
        val options                     = enums.values.map(_.entryName)
        val existingValues: Set[String] = decode(data).map(_.map(_.entryName)).getOrElse(Set.empty)
        checkboxes(
          fieldKey,
          options,
          existingValues,
          errors,
          messages
        ).some
      }

    }

  implicit val twirlUKAddressField: WebAsk[Html, UkAddress] = new WebAsk[Html, UkAddress] {
    implicit val a: Codec[Postcode]            = postcodeField
    implicit val b: Codec[AddressLine]         = mandatoryAddressField
    implicit val c: Codec[Option[AddressLine]] = optAddressField

    override val codec: Codec[UkAddress]                 = common.web.InferCodec.gen[UkAddress]
    def decode(out: Input): Either[ErrorTree, UkAddress] = codec.decode(out)
    def encode(in: UkAddress): Input                     = codec.encode(in)

    def render(
      pagekey: List[String],
      fieldKey: List[String],
      tell: Option[Html],
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Option[Html] =
      address(
        fieldKey,
        data,
        errors,
        messages,
        "UkAddress"
      ).some
  }

  implicit val twirlForeignAddressField: WebAsk[Html, ForeignAddress] = new WebAsk[Html, ForeignAddress] {
    implicit val a: Codec[CountryCode]         = twirlCountryCodeField
    implicit val b: Codec[AddressLine]         = mandatoryAddressField
    implicit val c: Codec[Option[AddressLine]] = optAddressField

    override val codec: Codec[ForeignAddress] = common.web.InferCodec.gen[ForeignAddress]

    def decode(out: Input): Either[ErrorTree, ForeignAddress] = codec.decode(out)
    def encode(in: ForeignAddress): Input                     = codec.encode(in)

    def render(
      pagekey: List[String],
      fieldKey: List[String],
      tell: Option[Html],
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Option[Html] =
      address(
        fieldKey,
        data,
        errors,
        messages
      ).some
  }

  implicit val twirlDateField: WebAsk[Html, LocalDate] =
    new WebAsk[Html, LocalDate] {

      def decode(out: Input): Either[ErrorTree, LocalDate] = {

        def intAtKey(key: String): Validated[Map[String, List[String]], Int] =
          Validated
            .fromOption(
              out.valueAt(key).flatMap(_.find(_.trim.nonEmpty)),
              Map("empty" -> List(key))
            )
            .andThen(x =>
              Validated
                .catchOnly[NumberFormatException](x.toInt)
                .leftMap(_ => Map("nan" -> List(key)))
            )
            .andThen(x =>
              Validated.cond(
                (key, x) match {
                  case ("day", n)   => n > 0 && n <= 31
                  case ("month", n) => n > 0 && n <= 12
                  case ("year", n)  => n.toString.length == 4
                  case _            => false
                },
                x,
                Map("invalid" -> List(key))
              )
            )

        (
          intAtKey("year"),
          intAtKey("month"),
          intAtKey("day")
        ).tupled match {
          case Validated.Valid((y, m, d)) =>
            Either
              .catchOnly[java.time.DateTimeException] {
                LocalDate.of(y, m, d)
              }
              .leftMap(_ => ErrorTree.oneErr(ErrorMsg("not-a-date")))
          case Validated.Invalid(errors)  =>
            (errors.get("empty"), errors.get("nan"), errors.get("invalid")) match {
              case (Some(empty), _, _)   => Left(ErrorMsg(empty.reverse.mkString("-and-") + ".empty").toTree)
              case (_, Some(nan), _)     => Left(ErrorMsg(nan.reverse.mkString("-and-") + ".nan").toTree)
              case (_, _, Some(invalid)) => Left(ErrorMsg(invalid.reverse.mkString("-and-") + ".invalid").toTree)
              case _                     =>
                logger.warn("Date validation should've been caught by and empty or nan case")
                Left(ErrorTree.oneErr(ErrorMsg("not-a-date")))
            }
        }
      }

      def encode(in: LocalDate): Input = Map(
        List("year")  -> in.getYear,
        List("month") -> in.getMonthValue,
        List("day")   -> in.getDayOfMonth
      ).view.mapValues(_.toString.pure[List]).toMap

      def render(
        pageKey: List[String],
        fieldKey: List[String],
        tell: Option[Html],
        path: Breadcrumbs,
        data: Input,
        errors: ErrorTree,
        messages: UniformMessages[Html]
      ): Option[Html] =
        date(
          fieldKey,
          data,
          errors,
          messages
        ).some
    }

  implicit lazy val askUnit: WebAsk[Html, Unit] = new WebAsk[Html, Unit] {
    def decode(out: Input): Either[ErrorTree, Unit] = Right(())
    def encode(in: Unit): Input                     = Input.empty

    def render(
      pageKey: List[String],
      fieldKey: List[String],
      tell: Option[Html],
      breadcrumbs: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Option[Html] = Some(standardField(pageKey, errors, messages)(tell.getOrElse(Html(""))))
  }

  def tellList[A](f: A => Html) = new WebTell[Html, WebAskList.ListingTable[A]] {
    def render(in: WebAskList.ListingTable[A], key: String, messages: UniformMessages[Html]): Option[Html] =
      Some(listing(key, in.value.map(f), messages))
  }

  implicit val tellListGroupCompany = tellList { groupCompany: GroupCompany =>
    Html(groupCompany.name)
  }

  def renderAnd(
    pageKey: List[String],
    fieldKey: List[String],
    tell: Option[Html],
    breadcrumbs: Breadcrumbs,
    data: Input,
    errors: ErrorTree,
    messages: UniformMessages[Html],
    members: Seq[(String, Html)]
  ): Html = Html(
    members.map { x =>
      x._2.toString
    }.mkString
  )

  def renderOr(
    pageKey: List[String],
    fieldKey: List[String],
    tell: Option[Html],
    breadcrumbs: Breadcrumbs,
    data: Input,
    errors: ErrorTree,
    messages: UniformMessages[Html],
    alternatives: Seq[(String, Option[Html])],
    selected: Option[String]
  ): Html =
    radios(
      fieldKey,
      alternatives.map {
        _._1
      },
      selected,
      errors,
      messages,
      alternatives.collect { case (name, Some(html)) =>
        name -> html
      }.toMap,
      tell
    )

  def messages(
    request: Request[AnyContent]
  ): UniformMessages[Html] =
    messagesApi.preferred(request).convertMessagesTwirlHtml(escapeHtml = false) |+|
      UniformMessages.bestGuess.map(HtmlFormat.escape)

  // N.b. this next line very useful for correcting the keys of missing content, leave for now
  //     UniformMessages.attentionSeeker.map(HtmlFormat.escape)

  override def pageChrome(
    keyList: List[String],
    errors: ErrorTree,
    html: Option[Html],
    breadcrumbs: List[String],
    request: Request[AnyContent],
    messages: UniformMessages[Html]
  ): Html = {

    // very crude method to determine if a page is a kickout - we
    // need a more robust/generic technique based upon different
    // behaviour for 'end' interactions
    def isKickout: Boolean = html.fold(false)(_.toString.contains("""<div class="kickout"""))

    val content: Html = if (isKickout) {
      html.get
    } else {
      formWrapper(
        keyList,
        errors,
        html.get,
        List(breadcrumbs.drop(1))
      )(messages, request)
    }

    val errorTitle: String = if (errors.isNonEmpty) s"${messages("common.error")}: " else ""
    val plainTextHeading   = Jsoup
      .parse(
        messages(
          keyList
            .mkString("-")
            .replaceAll("""\bmanage-companies-edit-\d+-company\b""", "manage-companies-edit-company")
            + ".heading"
        ).toString
      )
      .body
      .text

    layout(
      pageTitle =
        (errorTitle + s"$plainTextHeading - ${messages("common.title")} - ${messages("common.title.suffix")}").some
    )(content)(request, messages, appConfig)
  }

  def validatedVariant[BaseType](validated: ValidatedType[BaseType])(implicit
    baseForm: WebAsk[Html, BaseType]
  ): WebAsk[Html, BaseType @@ validated.Tag] =
    baseForm.simap { x =>
      Either.fromOption(validated.of(x), ErrorMsg("invalid").toTree)
    }(x => x: BaseType)

  def validatedString(
    validated: ValidatedType[String],
    maxLen: Int = Integer.MAX_VALUE
  )(implicit
    baseForm: WebAsk[Html, String]
  ): WebAsk[Html, String @@ validated.Tag] =
    baseForm.simap {
      case x if x.trim.isEmpty    => Left(ErrorMsg("required").toTree)
      case l if l.length > maxLen => Left(ErrorMsg("length.exceeded").toTree)
      case x                      => Either.fromOption(validated.of(x), ErrorMsg("invalid").toTree)
    }(x => x: String)

  def inlineOptionString(
    validated: ValidatedType[String],
    maxLen: Int = Integer.MAX_VALUE
  ): WebAsk[Html, Option[String @@ validated.Tag]] =
    twirlStringField.simap {
      case ""                     => Right(None)
      case l if l.length > maxLen => Left(ErrorMsg("length.exceeded").toTree)
      case x                      =>
        Either.fromOption(
          validated.of(x).map(Some(_)),
          ErrorMsg("invalid").toTree
        )
    } {
      case None    => ""
      case Some(x) => x
    }

  def validatedBigDecimal(
    validated: ValidatedType[BigDecimal],
    maxLen: Int = Integer.MAX_VALUE
  )(implicit
    baseForm: WebAsk[Html, BigDecimal]
  ): WebAsk[Html, BigDecimal @@ validated.Tag] =
    baseForm.simap {
      case l if l.precision > maxLen => Left(ErrorMsg("length.exceeded").toTree)
      case x                         => Either.fromOption(validated.of(x), ErrorMsg("invalid").toTree)
    }(x => x: BigDecimal)

  implicit lazy val postcodeField = validatedString(Postcode)(
    twirlStringFields(
      customRender = Some(stringField(_, _, _, _, _, "govuk-input--width-10"))
    )
  )

  implicit lazy val nesField                       = validatedVariant(NonEmptyString)
  implicit lazy val utrField                       = validatedString(UTR)
  implicit lazy val safeIdField                    = validatedString(SafeId)
  implicit lazy val emailField                     = validatedString(Email, 132)
  implicit lazy val percentField                   = validatedVariant(Percent)
  implicit lazy val moneyField                     = validatedBigDecimal(Money, 15)
  implicit lazy val accountNumberField             = validatedString(AccountNumber)
  implicit lazy val BuildingSocietyRollNumberField = inlineOptionString(BuildingSocietyRollNumber, 18)
  implicit lazy val accountNameField               = validatedString(AccountName, 35)
  implicit lazy val sortCodeField                  = validatedString(SortCode)(
    twirlStringFields(
      customRender = Some(stringField(_, _, _, _, _, "form-control form-control-1-4"))
    )
  )

  implicit lazy val ibanField             = validatedString(IBAN, 34)
  implicit lazy val companyNameField      = validatedString(CompanyName, 105)
  implicit lazy val mandatoryAddressField = validatedString(AddressLine, 35)
  implicit lazy val optAddressField       = inlineOptionString(AddressLine, 35)
  implicit lazy val restrictField         = validatedString(RestrictiveString, 35)

  implicit lazy val optUtrField: WebAsk[Html, Option[UTR]] = inlineOptionString(UTR)

  implicit lazy val intField: WebAsk[Html, Int] =
    twirlStringFields().simap(x =>
      {
        Rule.nonEmpty[String].apply(x) andThen
          Transformation.catchOnly[NumberFormatException]("not-a-number")(_.toInt)
      }.toEither
    )(_.toString)

  implicit lazy val floatField: WebAsk[Html, Float] =
    twirlStringFields(
      customRender = Some(stringField(_, _, _, _, _, "form-control govuk-input--width-4 govuk-input-z-index"))
    ).simap(x =>
      {
        Rule.nonEmpty[String].apply(x) andThen
          Transformation.catchOnly[NumberFormatException]("not-a-number")(_.toFloat)
      }.toEither
    )(_.toString)

  implicit lazy val longField: WebAsk[Html, Long] =
    twirlStringFields().simap(x =>
      {
        Rule.nonEmpty[String].apply(x.replace("%", "")) andThen
          Transformation.catchOnly[NumberFormatException]("not-a-number")(_.toLong)
      }.toEither
    )(_.toString)

  implicit lazy val bigdecimalField: WebAsk[Html, BigDecimal] =
    twirlStringFields(
      customRender = Some(stringField(_, _, _, _, _, "govuk-input-money govuk-input--width-10"))
    ).simap(x =>
      {
        Rule.nonEmpty[String].apply(x.replace(",", "").replace("£", "")) andThen
          Transformation.catchOnly[NumberFormatException]("not-a-number")(BigDecimal.apply)
      }.toEither
    )(_.toString)

}
