//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.protobuf

import com.google.protobuf.DescriptorProtos.EnumDescriptorProto

import scala.collection.JavaConversions.asScalaBuffer

object Enums {

  def enumContent(e: EnumDescriptorProto, `package`: String) = {
    val className = e.getName

    s"package ${`package`} {\n" +
      "\timport com.netease.protobuf.Enum;\n" +
      s"\tpublic final class $className implements Enum {\n" +
      enumConsts(e).mkString +
      "\t\tprivate var _id:int;\n" +
      "\t\tprivate var _name:String;\n" +
      s"\t\tpublic function $className(id:int, name:String) {\n" +
      "\t\t\t_id=id;\n" +
      "\t\t\t_name=name;\n" +
      "\t\t}\n\n" +
      "\t\tpublic function id():int {\n" +
      "\t\t\treturn _id;\n" +
      "\t\t}\n" +
      "\t\tpublic function name():String {\n" +
      "\t\t\treturn _name;\n" +
      "\t\t}\n" +
      s"\t\tpublic static const values : Vector.<$className> = new <$className>[\n" +
      enumValues(e) + "\n" +
      s"\t\t];\n" +
      "\t\tpublic static const valuesById : Object = {\n" +
      enumIdToValues(e) + "\n" +
      "\t\t}\n" +
      "\t}\n" +
      "}\n"
  }

  def enumConsts(e: EnumDescriptorProto) =
    for (value ← e.getValueList;
         valueName = value.getName;
         className = e.getName) yield
      s"\t\tpublic static const $valueName: $className = new $className(${value.getNumber}, '${value.getName}');\n"

  def enumValues(e: EnumDescriptorProto) =
    e.getValueList.map(v ⇒ "\t\t\t" + v.getName).mkString(",\n")

  def enumIdToValues(e: EnumDescriptorProto) =
    e.getValueList.map(v ⇒ "\t\t\t" + v.getNumber + ": " + v.getName).mkString(",\n")
}
