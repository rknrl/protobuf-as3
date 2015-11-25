name := "plugin"

version := "1.0"

scalaVersion := "2.11.7"

import sbtprotobuf.{ProtobufPlugin=>PB}

Seq(PB.protobufSettings: _*)

version in protobufConfig := "2.4.1"

javaSource in PB.protobufConfig <<= (baseDirectory in Compile)(_ / "src/generated/java")

protocOptions in PB.protobufConfig += "--proto_path=/Users/tolyayanot/dev/rknrl/protobuf-as2/plugin/src/main/proto"

test in assembly := {}

assemblyJarName in assembly := "protobuf-as3.jar"

mainClass in assembly := Some("ru.rknrl.protobuf.Main")