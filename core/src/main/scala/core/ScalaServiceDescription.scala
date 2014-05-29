package core

import Text._

object ScalaUtil {

  // TODO: Use GeneratorUtil.formatComment here
  def textToComment(text: String) = {
    val lines = text.split("\n")
    lines.mkString("/**\n * ", "\n * ", "\n */\n")
  }

  def fieldsToArgList(fields: Seq[String]) = {
    fields.map(_.indent).mkString("\n", ",\n", "\n")
  }
}

class ScalaServiceDescription(serviceDescription: ServiceDescription) {

  val name = safeName(serviceDescription.name)

  val description = serviceDescription.description.map(ScalaUtil.textToComment(_)).getOrElse("")

  val models = serviceDescription.models.map { new ScalaModel(_) }

  val resources = serviceDescription.resources.map { new ScalaResource(_) }

}

class ScalaModel(model: Model) {

  val name = underscoreToInitCap(model.name)

  val plural = underscoreToInitCap(model.plural)

  val description = model.description.map(ScalaUtil.textToComment).getOrElse("")

  val fields = model.fields.map { new ScalaField(_) }

  val argList = ScalaUtil.fieldsToArgList(fields.map(_.definition))

}

class ScalaResource(resource: Resource) {
  val model = new ScalaModel(resource.model)

  val path = resource.path

  val operations = resource.operations.map { new ScalaOperation(_) }
}

class ScalaOperation(operation: Operation) {

  val method: String = operation.method

  val path: String = operation.path

  val description: String = {
    operation.description.map(ScalaUtil.textToComment).getOrElse("")
  }

  val parameters = operation.parameters.map { new ScalaParameter(_) }

  val name: String = {
    val sanitizedPath: String = {
      val snakeCase = path.replaceAll("^/[^/]*", "").replaceAll("/:", "_")
      safeName(underscoreToInitCap(snakeCase))
    }
    method.toLowerCase + sanitizedPath
  }

  val argList: String = ScalaUtil.fieldsToArgList(parameters.map(_.definition))

  val responses = operation.responses.map { new ScalaResponse(_) }
}

class ScalaResponse(response: Response) {
  def code = response.code

  def datatype = underscoreToInitCap(response.datatype)

  def multiple = response.multiple
}

// TODO support multiple
class ScalaField(field: Field) {

  def name: String = snakeToCamelCase(field.name)

  def originalName: String = field.name

  def datatype: ScalaDataType = field.fieldtype match {

    case t: PrimitiveFieldType => ScalaDataType(t.datatype)
    case m: ModelFieldType => new ScalaDataType(underscoreToInitCap(m.model.name))
    case r: ReferenceFieldType => new ScalaDataType(underscoreToInitCap(s"Reference[#{underscoreToInitCap(r.model.name)}]"))

  }

  def description: String = field.description.map(ScalaUtil.textToComment).getOrElse("")

  def isOption: Boolean = !field.required || field.default.nonEmpty

  def typeName: String = if (isOption) s"Option[${datatype.name}]" else datatype.name

  def definition: String = {
    val decl = s"$description$name: $typeName"
    if (isOption) decl + " = None" else decl
  }
}

// TODO support multiple
class ScalaParameter(param: Parameter) {

  def name: String = snakeToCamelCase(param.name)

  def originalName: String = param.name

  def datatype: ScalaDataType = param.paramtype match {

    case t: PrimitiveParameterType => ScalaDataType(t.datatype)
    case m: ModelParameterType => new ScalaDataType(underscoreToInitCap(m.model.name))

  }

  def description: String = param.description.map(ScalaUtil.textToComment).getOrElse("")

  def isOption: Boolean = !param.required || param.default.nonEmpty

  def typeName: String = if (isOption) s"Option[${datatype.name}]" else datatype.name

  def definition: String = {
    val decl = s"$description$name: $typeName"
    if (isOption) decl + " = None" else decl
  }

  def location = param.location
}

class ScalaDataType(val name: String)

object ScalaDataType {

  case object ScalaStringType extends ScalaDataType("java.lang.String")
  case object ScalaIntegerType extends ScalaDataType("scala.Int")
  case object ScalaLongType extends ScalaDataType("scala.Long")
  case object ScalaBooleanType extends ScalaDataType("scala.Boolean")
  case object ScalaDecimalType extends ScalaDataType("scala.BigDecimal")
  case object ScalaUnitType extends ScalaDataType("scala.Unit")
  case object ScalaUuidType extends ScalaDataType("java.util.UUID")
  case object ScalaDateTimeIso8601Type extends ScalaDataType("org.joda.time.DateTime")
  case object ScalaMoneyIso4217Type extends ScalaDataType("Money")

  def apply(datatype: Datatype): ScalaDataType = datatype match {
    case Datatype.StringType => ScalaStringType
    case Datatype.IntegerType => ScalaIntegerType
    case Datatype.LongType => ScalaLongType
    case Datatype.BooleanType => ScalaBooleanType
    case Datatype.DecimalType => ScalaDecimalType
    case Datatype.UnitType => ScalaUnitType
    case Datatype.UuidType => ScalaUuidType
    case Datatype.DateTimeIso8601Type => ScalaDateTimeIso8601Type
    case Datatype.MoneyIso4217Type => ScalaMoneyIso4217Type
  }

}
