//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package com.netease.protobuf {
import flash.utils.*;

public function messageToString(message:Object):String {
    var s:String = getQualifiedClassName(message) + "(\n";
    const descriptor:XML = describeType(message);
    for each (var getter:String in descriptor.accessor.(@access != "writeonly").@name) {
        if (getter.search(/^has[A-Z]/) != -1) {
            continue;
        }
        s += fieldToString(message, descriptor, getter);
    }
    for each (var field:String in descriptor.variable.@name) {
        if (field.search(/^_/) != -1) {
            continue;
        }
        s += fieldToString(message, descriptor, field);
    }
    for (var k:* in message) {
        s += k + "=" + message[k] + ";\n";
    }
    s += ")";
    return s;
}
}

import com.netease.protobuf.*;

function fieldToString(message:Object, descriptor:XML, name:String):String {
    var hasField:String = "has" + name.charAt(0).toUpperCase() + name.substr(1);
    if (descriptor.accessor.(@name == hasField).length() != 0 && !message[hasField]) {
        return "";
    }
    var field:* = message[name];
    if (field != null && !(field is IMessage) && field is Array && field.length == 0) {
        return "";
    }
    return name + "=" + field + ";\n";
}
