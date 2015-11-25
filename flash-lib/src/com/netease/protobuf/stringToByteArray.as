//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package com.netease.protobuf {
import flash.utils.ByteArray;

public function stringToByteArray(s:String):ByteArray {
    const ba:ByteArray = new ByteArray();
    for (var i:uint = 0; i < s.length; ++i) {
        ba.writeByte(s.charCodeAt(i));
    }
    return ba;
}
}
