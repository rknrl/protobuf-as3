//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.protobuf {
import flash.utils.*;

// Based on com.netease.protobuf (c) Yang Bo (pop.atry@gmail.com)
public final class WriteUtils {
    private static function writeVarint64(output:IDataOutput,
                                          low:uint, high:uint):void {
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

    public static function writeTag(output:IDataOutput, wireType:uint, fieldNumber:uint):void {
        writeUInt32(output, (fieldNumber << 3) | wireType);
    }

    public static function writeDouble(output:IDataOutput, value:Number):void {
        output.writeDouble(value);
    }

    public static function writeFloat(output:IDataOutput, value:Number):void {
        output.writeFloat(value);
    }

    public static function writeInt64AsNumber(output:IDataOutput, value:Number):void {
        writeInt64(output, Int64.fromNumber(value));
    }

    public static function writeInt64(output:IDataOutput, value:Int64):void {
        writeVarint64(output, value.low, value.high);
    }

    public static function writeUInt64AsNumber(output:IDataOutput, value:Number):void {
        writeUInt64(output, UInt64.fromNumber(value));
    }

    public static function writeUInt64(output:IDataOutput, value:UInt64):void {
        writeVarint64(output, value.low, value.high);
    }

    public static function writeInt32(output:IDataOutput, value:int):void {
        if (value < 0) {
            writeVarint64(output, value, 0xFFFFFFFF)
        } else {
            writeUInt32(output, value);
        }
    }

    public static function writeFixed64AsNumber(output:IDataOutput, value:Number):void {
        writeFixed64(output, UInt64.fromNumber(value));
    }

    public static function writeFixed64(output:IDataOutput, value:UInt64):void {
        output.writeUnsignedInt(value.low);
        output.writeUnsignedInt(value.high);
    }

    public static function writeFixed32(output:IDataOutput, value:uint):void {
        output.writeUnsignedInt(value);
    }

    public static function writeBool(output:IDataOutput, value:Boolean):void {
        output.writeByte(value ? 1 : 0);
    }

    public static function writeString(output:IDataOutput, value:String):void {
        const byteArray: ByteArray = new ByteArray();
        byteArray.endian = Endian.LITTLE_ENDIAN;
        byteArray.writeUTFBytes(value);
        writeUInt32(output, byteArray.length);
        output.writeBytes(byteArray);
    }

    public static function writeBytes(output:IDataOutput, value:ByteArray):void {
        writeUInt32(output, value.length);
        output.writeBytes(value);
    }

    public static function writeUInt32(output:IDataOutput, value:uint):void {
        for (; ;) {
            if (value < 0x80) {
                output.writeByte(value);
                return;
            } else {
                output.writeByte((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    public static function writeEnumId(output:IDataOutput, value:Enum):void {
        writeInt32(output, value.id);
    }

    public static function writeSFixed32(output:IDataOutput, value:int):void {
        output.writeInt(value);
    }

    public static function writeSFixed64AsNumber(output:IDataOutput, value:Number):void {
        writeSFixed64(output, Int64.fromNumber(value));
    }

    public static function writeSFixed64(output:IDataOutput, value:Int64):void {
        output.writeUnsignedInt(value.low);
        output.writeInt(value.high);
    }

    public static function writeSInt32(output:IDataOutput, value:int):void {
        writeUInt32(output, ZigZag.encode32(value))
    }

    public static function writeSInt64AsNumber(output:IDataOutput, value:Number):void {
        writeSInt64(output, Int64.fromNumber(value));
    }

    public static function writeSInt64(output:IDataOutput, value:Int64):void {
        writeVarint64(output,
                ZigZag.encode64low(value.low, value.high),
                ZigZag.encode64high(value.low, value.high))
    }

    public static function writeMessage(output:IDataOutput, value:Message):void {
        const byteArray: ByteArray = new ByteArray();
        byteArray.endian = Endian.LITTLE_ENDIAN;
        value.writeTo(byteArray);
        writeUInt32(output, byteArray.length);
        output.writeBytes(byteArray);
    }
}
}