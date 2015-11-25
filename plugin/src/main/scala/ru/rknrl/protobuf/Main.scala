//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.protobuf

import java.io.{PrintWriter, StringWriter}

import com.google.protobuf.DescriptorProtos.{DescriptorProto, EnumDescriptorProto}
import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorResponse, CodeGeneratorRequest}
import ru.rknrl.protobuf.Enums.enumContent
import ru.rknrl.protobuf.Messages.messageContent

import scala.collection.JavaConversions.{asJavaIterable, asScalaBuffer}

object Main {
  def main(args: Array[String]) {
    val request = CodeGeneratorRequest.parseFrom(System.in)
    createResponse(request).writeTo(System.out)
    System.out.flush()
  }

  def createResponse(request: CodeGeneratorRequest) =
    try {
      CodeGeneratorResponse.newBuilder.addAllFile(createFiles(request)).build
    } catch {
      case t: Throwable ⇒
        val sw = new StringWriter
        t.printStackTrace(new PrintWriter(sw))
        CodeGeneratorResponse.newBuilder.setError(sw.toString).build
    }

  def createFiles(request: CodeGeneratorRequest) = {
    val toGenerate = request.getProtoFileList
      .filter(f ⇒ request.getFileToGenerateList.contains(f.getName))

    val enums = toGenerate.flatMap(f ⇒ f.getEnumTypeList.map(e ⇒ createEnumFile(e, f.getPackage)))

    val messages = toGenerate.flatMap(f ⇒ f.getMessageTypeList.map(m ⇒ createMessageFile(m, f.getPackage)))

    enums ++ messages
  }

  def packageToPath(`package`: String) =
    if (`package`.isEmpty)
      ""
    else
      `package`.replace('.', '/') + "/"

  def createEnumFile(e: EnumDescriptorProto, `package`: String) =
    CodeGeneratorResponse.File.newBuilder
      .setName(packageToPath(`package`) + e.getName + ".as")
      .setContent(enumContent(e, `package`))
      .build

  def createMessageFile(d: DescriptorProto, `package`: String) =
    CodeGeneratorResponse.File.newBuilder
      .setName(packageToPath(`package`) + d.getName + ".as")
      .setContent(messageContent(d, `package`))
      .build
}
