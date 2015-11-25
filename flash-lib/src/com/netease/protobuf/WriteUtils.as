//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package com.netease.protobuf {
import flash.utils.*;

public final class WriteUtils {
    private static function writeVarint64(output:WritingBuffer, low:uint, high:uint):void {
        if (high == 0) {
            writeUInt32(output, low);
        } else {
            for (var i:uint = 0; i < 4; ++i) {
                output.writeByte((low & 0x7F) | 0x80);
                low >>>= 7;
            }
            if ((high & (0xFFFFFFF << 3)) == 0) {
                output.writeByte((high << 4) | low);
            } else {
                output.writeByte((((high << 4) | low) & 0x7F) | 0x80);
                writeUInt32(output, high >>> 3);
            }
        }
    }

    public static function writeTag(output:WritingBuffer, wireType:uint, number:uint):void {
        writeUInt32(output, (number << 3) | wireType);
    }

    public static function writeDouble(output:WritingBuffer, value:Number):void {
        output.writeDouble(value);
    }

    public static function writeFloat(output:WritingBuffer, value:Number):void {
        output.writeFloat(value);
    }

    public static function writeInt64(output:WritingBuffer, value:Number):void {
        const int64:Int64 = Int64.fromNumber(value);
        writeVarint64(output, int64.low, int64.high);
    }

    public static function writeUInt64(output:WritingBuffer, value:Number):void {
        const uint64:UInt64 = UInt64.fromNumber(value);
        writeVarint64(output, uint64.low, uint64.high);
    }

    public static function writeInt32(output:WritingBuffer, value:int):void {
        if (value < 0) {
            writeVarint64(output, value, 0xFFFFFFFF);
        } else {
            writeUInt32(output, value);
        }
    }

    public static function writeFixed64(output:WritingBuffer, value:Number):void {
        const int64:Int64 = Int64.fromNumber(value);
        output.writeUnsignedInt(int64.low);
        output.writeInt(int64.high);
    }

    public static function writeFixed32(output:WritingBuffer, value:int):void {
        output.writeInt(value);
    }

    public static function writeBool(output:WritingBuffer, value:Boolean):void {
        output.writeByte(value ? 1 : 0);
    }

    public static function writeString(output:WritingBuffer, value:String):void {
        const i:uint = output.beginBlock();
        output.writeUTFBytes(value);
        output.endBlock(i);
    }

    public static function writeBytes(output:WritingBuffer, value:ByteArray):void {
        writeUInt32(output, value.length);
        output.writeBytes(value);
    }

    public static function writeUInt32(output:WritingBuffer, value:uint):void {
        for (; ;) {
            if ((value & ~0x7F) == 0) {
                output.writeByte(value);
                return;
            } else {
                output.writeByte((value & 0x7F) | 0x80)
                value >>>= 7
            }
        }
    }

    public static function writeEnum(output:WritingBuffer, value:Enum):void {
        writeInt32(output, value.id());
    }

    public static function writeSFixed32(output:WritingBuffer, value:int):void {
        writeFixed32(output, value);
    }

    public static function writeSFixed64(output:WritingBuffer, value:Number):void {
        const int64:Int64 = Int64.fromNumber(value);
        output.writeUnsignedInt(ZigZag.encode64low(int64.low, int64.high));
        output.writeUnsignedInt(ZigZag.encode64high(int64.low, int64.high));
    }

    public static function writeSInt32(output:WritingBuffer, value:int):void {
        writeUInt32(output, ZigZag.encode32(value));
    }

    public static function writeSInt64(output:WritingBuffer, value:Number):void {
        const int64:Int64 = Int64.fromNumber(value);
        writeVarint64(output,
                ZigZag.encode64low(int64.low, int64.high),
                ZigZag.encode64high(int64.low, int64.high))
    }

    public static function writeMessage(output:WritingBuffer, value:IMessage):void {
        const i:uint = output.beginBlock();
        value.writeToBuffer(output);
        output.endBlock(i);
    }

    public static function writePackedRepeated(output:WritingBuffer,
                                               writeFunction:Function, value:Array):void {
        const i:uint = output.beginBlock();
        for (var j:uint = 0; j < value.length; j++) {
            writeFunction(output, value[j]);
        }
        output.endBlock(i);
    }
}
}
