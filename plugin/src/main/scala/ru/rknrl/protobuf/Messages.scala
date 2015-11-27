//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.protobuf

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.{LABEL_OPTIONAL, LABEL_REPEATED, LABEL_REQUIRED}
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type._
import com.google.protobuf.DescriptorProtos.{DescriptorProto, FieldDescriptorProto}

import scala.collection.JavaConversions.asScalaBuffer

object Messages {

  def fullPathClass(f: FieldDescriptorProto) =
    if (f.getTypeName.charAt(0) == '.')
      f.getTypeName.substring(1)
    else
      f.getTypeName

  /** MyWord => myWord */
  def uncapitalize(s: String): String =
    if (s == null) null
    else if (s.length == 0) ""
    else if (s.charAt(0).isLower) s
    else {
      val chars = s.toCharArray
      chars(0) = chars(0).toLower
      new String(chars)
    }

  def className(d: DescriptorProto) = d.getName

  def fieldNumber(f: FieldDescriptorProto) = f.getNumber

  def fieldName(f: FieldDescriptorProto) = uncapitalize(f.getName)

  def upperFieldName(f: FieldDescriptorProto) = f.getName.capitalize

  def privateFieldName(f: FieldDescriptorProto) = "_" + fieldName(f)

  def isNullableType(as3Type: String) =
    as3nullableType(as3Type) == as3Type

  def as3nullableType(as3Type: String) =
    as3Type match {
      case "Number" ⇒ "Num"
      case "int" ⇒ "Int"
      case "uint" ⇒ "UInt"
      case "Boolean" ⇒ "Bool"
      case _ ⇒ as3Type
    }

  def ioMethodPostfix(f: FieldDescriptorProto) =
    f.getType match {
      case TYPE_DOUBLE ⇒ "Double"
      case TYPE_FIXED64 ⇒ "Fixed64AsNumber"
      case TYPE_SFIXED64 ⇒ "SFixed64AsNumber"
      case TYPE_FLOAT ⇒ "Float"
      case TYPE_FIXED32 ⇒ "Fixed32"
      case TYPE_SFIXED32 ⇒ "SFixed32"
      case TYPE_INT32 ⇒ "Int32"
      case TYPE_SINT32 ⇒ "SInt32"
      case TYPE_UINT32 ⇒ "UInt32"
      case TYPE_BOOL ⇒ "Bool"
      case TYPE_INT64 ⇒ "Int64AsNumber"
      case TYPE_UINT64 ⇒ "UInt64AsNumber"
      case TYPE_SINT64 ⇒ "SInt64AsNumber"
      case TYPE_ENUM ⇒ "EnumId"
      case TYPE_STRING ⇒ "String"
      case TYPE_MESSAGE ⇒ "Message"
      case TYPE_BYTES ⇒ "Bytes"
    }

  def writeMethodName(f: FieldDescriptorProto) =
    "WriteUtils.write" + ioMethodPostfix(f)

  def readMethodName(f: FieldDescriptorProto) =
    "ReadUtils.read" + ioMethodPostfix(f)

  def as3WireConst(f: FieldDescriptorProto) =
    f.getType match {
      case TYPE_DOUBLE |
           TYPE_FIXED64 |
           TYPE_SFIXED64 ⇒ "FIXED_64_BIT"

      case TYPE_FLOAT |
           TYPE_FIXED32 |
           TYPE_SFIXED32 ⇒ "FIXED_32_BIT"

      case TYPE_INT32 |
           TYPE_SINT32 |
           TYPE_UINT32 |
           TYPE_BOOL |
           TYPE_INT64 |
           TYPE_UINT64 |
           TYPE_SINT64 |
           TYPE_ENUM ⇒ "VARINT"

      case TYPE_STRING |
           TYPE_MESSAGE |
           TYPE_BYTES ⇒ "LENGTH_DELIMITED"
    }

  private def as3Type(f: FieldDescriptorProto) =
    f.getType match {
      case TYPE_DOUBLE |
           TYPE_FLOAT ⇒ "Number"

      case TYPE_INT32 |
           TYPE_FIXED32 |
           TYPE_SFIXED32 |
           TYPE_SINT32 ⇒ "int"

      case TYPE_UINT32 ⇒ "uint"

      case TYPE_BOOL ⇒ "Boolean"

      case TYPE_INT64 |
           TYPE_FIXED64 |
           TYPE_SFIXED64 |
           TYPE_SINT64 |
           TYPE_UINT64 ⇒ "Number"

      case TYPE_STRING ⇒ "String"

      case TYPE_MESSAGE |
           TYPE_ENUM ⇒ fullPathClass(f)

      case TYPE_BYTES ⇒ "ByteArray"
    }

  def fieldType(f: FieldDescriptorProto) =
    f.getLabel match {
      case LABEL_OPTIONAL ⇒ as3nullableType(as3Type(f))
      case LABEL_REQUIRED ⇒ as3Type(f)
      case LABEL_REPEATED ⇒ "Vector.<" + as3Type(f) + ">";
    }

  def messageContent(d: DescriptorProto, `package`: String) =
    s"package ${`package`} {\n" +
      imports(d) +
      s"\tpublic final class ${className(d)} implements Message {\n" +
      messageConstructor(d) +
      fields(d) +
      write(d) +
      writeDelimitedTo(d) +
      read(d) +
      writeToString(d) +
      "\t}\n" +
      "}\n"

  def imports(d: DescriptorProto) =
    "\timport ru.rknrl.protobuf.*;\n" +
      "\timport flash.utils.Endian;\n" +
      "\timport flash.utils.IDataInput;\n" +
      "\timport flash.utils.IDataOutput;\n" +
      "\timport flash.errors.IOError;\n"

  def messageConstructor(d: DescriptorProto) =
    "\t\tpublic function " + className(d) + "(" + constructorParams(d) + ") {\n" +
      constructorBody(d) +
      "\t\t}\n\n"

  def constructorParams(d: DescriptorProto) =
    d.getFieldList.map(f ⇒ fieldName(f) + ": " + fieldType(f)).mkString(",")

  def constructorBody(d: DescriptorProto) =
    d.getFieldList.map(f ⇒ "\t\t\t" + privateFieldName(f) + " = " + fieldName(f) + ";\n").mkString

  def fields(d: DescriptorProto) =
    d.getFieldList.map(f ⇒ f.getLabel match {
      case LABEL_REQUIRED | LABEL_REPEATED ⇒ requiredField(f)
      case LABEL_OPTIONAL ⇒ optionalField(f)
    }).mkString

  def requiredField(f: FieldDescriptorProto) =
    "\t\tprivate var " + privateFieldName(f) + ":" + fieldType(f) + ";\n\n" +
      "\t\tpublic function get " + fieldName(f) + "(): " + fieldType(f) + "{\n" +
      "\t\t\treturn " + privateFieldName(f) + ";\n" +
      "\t\t}\n\n"

  def optionalField(f: FieldDescriptorProto) =
    "\t\tprivate var " + privateFieldName(f) + ":" + fieldType(f) + ";\n\n" +
      "\t\tpublic function get has" + upperFieldName(f) + "():Boolean {\n" +
      "\t\t\treturn " + privateFieldName(f) + " != null;\n" +
      "\t\t}\n\n" +
      optionalFieldGet(f) +
      "\t\tpublic function get " + fieldName(f) + "():" + as3Type(f) + " {\n" +
      "\t\t\treturn " + optionalFieldValue(f) + ";\n" +
      "\t\t}\n\n"

  def optionalFieldGet(f: FieldDescriptorProto) =
    if (!isNullableType(as3Type(f)))
      "\t\tpublic function get" + upperFieldName(f) + "():" + fieldType(f) + " {\n" +
        "\t\t\treturn " + privateFieldName(f) + ";\n" +
        "\t\t}\n\n"
    else
      ""

  def optionalFieldValue(f: FieldDescriptorProto) =
    if (isNullableType(as3Type(f)))
      privateFieldName(f)
    else
      privateFieldName(f) + ".value"

  def write(d: DescriptorProto) =
    "\t\tpublic final function writeTo(output:IDataOutput):void {\n" +
      writeFields(d) +
      "\t\t}\n\n"

  def writeFields(d: DescriptorProto) =
    d.getFieldList.map(f ⇒ f.getLabel match {
      case LABEL_REQUIRED ⇒ writeRequired(f)
      case LABEL_OPTIONAL ⇒ writeOptional(f)
      case LABEL_REPEATED ⇒ writeRepeated(f)
    }).mkString


  def writeRequired(f: FieldDescriptorProto) =
    "\t\t\tWriteUtils.writeTag(output, WireType." + as3WireConst(f) + ", " + fieldNumber(f) + ");\n" +
      "\t\t\t" + writeMethodName(f) + "(output, " + fieldName(f) + ");\n"

  def writeOptional(f: FieldDescriptorProto) =
    "\t\t\tif (has" + upperFieldName(f) + ") {\n" +
      "\t\t\t\tWriteUtils.writeTag(output, WireType." + as3WireConst(f) + ", " + fieldNumber(f) + ");\n" +
      "\t\t\t\t" + writeMethodName(f) + "(output, " + optionalFieldValue(f) + ");\n" +
      "\t\t\t}\n"

  def writeRepeated(f: FieldDescriptorProto) = {
    if (f.hasOptions && f.getOptions.hasPacked)
      throw new IllegalArgumentException("Repeated packed option is not supported")

    val index = fieldName(f) + "$index"

    "\t\t\tfor (var " + index + ":uint = 0; " + index + " < " + fieldName(f) + ".length; " + index + "++) {\n" +
      "\t\t\t\tWriteUtils.writeTag(output, WireType." + as3WireConst(f) + ", " + fieldNumber(f) + ");\n" +
      "\t\t\t\t" + writeMethodName(f) + "(output, " + fieldName(f) + "[" + index + "]);\n" +
      "\t\t\t}\n"
  }

  def writeDelimitedTo(d: DescriptorProto) =
    "\t\tpublic final function writeDelimitedTo(output:IDataOutput):void {\n" +
      "\t\t\tWriteUtils.writeMessage(output, this);\n" +
      "\t\t}\n\n"

  def read(d: DescriptorProto) =
    "\t\tpublic static function parseDelimitedFrom(input:IDataInput):" + className(d) + " {\n" +
      "\t\t\tconst length: uint = ReadUtils.readUInt32(input);\n" +
      "\t\t\tif (input.bytesAvailable < length) {\n" +
      "\t\t\t\tthrow new IOError('Invalid message length: ' + length);\n" +
      "\t\t\t}\n" +
      "\t\t\tconst bytesAfterSlice: uint = input.bytesAvailable - length;\n" +
      readInitVars(d) +
      "\t\t\twhile (input.bytesAvailable > bytesAfterSlice) {\n" +
      "\t\t\t\tvar tag:uint = ReadUtils.readUInt32(input);\n" +
      "\t\t\t\tswitch (tag >>> 3) {\n" +
      readFields(d) +
      "\t\t\t\tdefault:\n" +
      "\t\t\t\t\tReadUtils.skip(input, tag & 7);\n" +
      "\t\t\t\t}\n" +
      "\t\t\t}\n" +
      "\t\t\tif (input.bytesAvailable != bytesAfterSlice) {\n" +
      "\t\t\t\tthrow new IOError('Invalid nested message');\n" +
      "\t\t\t}\n" +
      readRequiredChecks(d) +
      readReturn(d) +
      "\t\t}\n\n"

  def readInitVars(d: DescriptorProto) =
    d.getFieldList.map(f ⇒ f.getLabel match {
      case LABEL_REQUIRED | LABEL_OPTIONAL ⇒ readInitRequiredVar(f)
      case LABEL_REPEATED ⇒ readInitRepeatedVar(f)
    }).mkString

  def fieldCountName(f: FieldDescriptorProto) = fieldName(f) + "$count"

  def readInitRequiredVar(f: FieldDescriptorProto) =
    "\t\t\tvar " + fieldName(f) + ":" + fieldType(f) + ";\n" +
      "\t\t\tvar " + fieldCountName(f) + ":uint = 0;\n"

  def readInitRepeatedVar(f: FieldDescriptorProto) =
    "\t\t\tvar " + fieldName(f) + ":" + fieldType(f) + " = new " + fieldType(f) + ";\n"

  def readFields(d: DescriptorProto) =
    d.getFieldList.map(f ⇒
      "\t\t\t\tcase " + Integer.toString(f.getNumber) + ":\n" +
        (f.getLabel match {
          case LABEL_REQUIRED | LABEL_OPTIONAL ⇒ checkAndIncreaseCount(f, d) + readField(f)
          case LABEL_REPEATED ⇒ readRepeated(f)
        }) +
        "\t\t\t\t\tbreak;\n"
    ).mkString

  def checkAndIncreaseCount(f: FieldDescriptorProto, d: DescriptorProto) =
    "\t\t\t\t\tif (" + fieldCountName(f) + " != 0) {\n" +
      "\t\t\t\t\t\tthrow new IOError('Bad data format: " + className(d) + '.' + fieldName(f) + " cannot be set twice.');\n" +
      "\t\t\t\t\t}\n" +
      "\t\t\t\t\t" + fieldCountName(f) + "++;\n"

  def readField(f: FieldDescriptorProto) =
    f.getType match {
      case TYPE_MESSAGE ⇒
        "\t\t\t\t\t" + fieldName(f) + " =  " + as3Type(f) + ".parseDelimitedFrom(input);\n"

      case TYPE_ENUM ⇒
        "\t\t\t\t\t" + fieldName(f) + " = " + as3Type(f) + ".valuesById[ReadUtils.readEnumId(input)];\n"

      case _ ⇒
        if (f.getLabel == LABEL_OPTIONAL && !isNullableType(as3Type(f)))
          "\t\t\t\t\t" + fieldName(f) + " = new " + fieldType(f) + "(" + readMethodName(f) + "(input));\n"
        else
          "\t\t\t\t\t" + fieldName(f) + " = " + readMethodName(f) + "(input);\n"
    }

  def readRepeated(f: FieldDescriptorProto) =
    f.getType match {
      case TYPE_MESSAGE ⇒ readRepeatedMessage(f)

      case TYPE_ENUM ⇒ readRepeatedEnum(f)

      case _ ⇒ "\t\t\t\t\t" + fieldName(f) + ".push(" + readMethodName(f) + "(input));\n"
    }

  def readRepeatedMessage(f: FieldDescriptorProto) =
    "\t\t\t\t\t" + fieldName(f) + ".push(" + as3Type(f) + ".parseDelimitedFrom(input));\n"

  def readRepeatedEnum(f: FieldDescriptorProto) =
    "\t\t\t\t\t" + fieldName(f) + ".push(" + as3Type(f) + ".valuesById[" + readMethodName(f) + "(input)]);\n"

  def readRequiredChecks(d: DescriptorProto) =
    d.getFieldList.filter(_.getLabel == LABEL_REQUIRED).map(f ⇒ readRequiredCheck(f, d)).mkString

  def readRequiredCheck(f: FieldDescriptorProto, d: DescriptorProto) =
    "\t\t\tif (" + fieldCountName(f) + " != 1) {\n" +
      "\t\t\t\tthrow new IOError('Bad data format: " + className(d) + '.' + fieldName(f) + " must be set');\n" +
      "\t\t\t}\n"

  def readReturn(d: DescriptorProto) =
    "\t\t\treturn new " + className(d) + "(" + readReturnParams(d).mkString(", ") + ")\n"

  def readReturnParams(d: DescriptorProto) = d.getFieldList.map(fieldName)

  def writeToString(d: DescriptorProto) =
    "\t\tpublic function toString():String {\n" +
      s"\t\t\treturn '${className(d)}{\\n" +
      toStringFields(d) +
      "}'\n" +
      "\t\t}\n\n"

  def toStringFields(d: DescriptorProto) =
    d.getFieldList.map(f ⇒ fieldName(f) + "='+String(" + privateFieldName(f) + ")+'\\n").mkString

}