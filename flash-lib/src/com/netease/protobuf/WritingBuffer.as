//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package com.netease.protobuf {
import flash.errors.*;
import flash.utils.*;

public final class WritingBuffer extends ByteArray {
    public function WritingBuffer() {
        endian = Endian.LITTLE_ENDIAN
    }

    private const slices:ByteArray = new ByteArray();

    public function beginBlock():uint {
        slices.writeUnsignedInt(position);
        const beginSliceIndex:uint = slices.length;
        if (beginSliceIndex % 8 != 4) {
            throw new IllegalOperationError();
        }
        slices.writeDouble(0);
        slices.writeUnsignedInt(position);
        return beginSliceIndex;
    }

    public function endBlock(beginSliceIndex:uint):void {
        if (slices.length % 8 != 0) {
            throw new IllegalOperationError();
        }
        slices.writeUnsignedInt(position);
        slices.position = beginSliceIndex + 8;
        const beginPosition:uint = slices.readUnsignedInt();
        slices.position = beginSliceIndex;
        slices.writeUnsignedInt(position);
        WriteUtils.writeUInt32(this, position - beginPosition);
        slices.writeUnsignedInt(position);
        slices.position = slices.length;
        slices.writeUnsignedInt(position);
    }

    public function toNormal(output:IDataOutput):void {
        if (slices.length % 8 != 0) {
            throw new IllegalOperationError();
        }
        slices.position = 0;
        var begin:uint = 0;
        while (slices.bytesAvailable > 0) {
            var end:uint = slices.readUnsignedInt();
            if (end > begin) {
                output.writeBytes(this, begin, end - begin);
            } else if (end < begin) {
                throw new IllegalOperationError();
            }
            begin = slices.readUnsignedInt();
        }
        if (begin < length) {
            output.writeBytes(this, begin);
        }
    }
}
}
