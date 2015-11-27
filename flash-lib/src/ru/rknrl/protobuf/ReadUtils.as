//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.protobuf {
import flash.errors.*;
import flash.utils.*;

// Based on com.netease.protobuf (c) Yang Bo (pop.atry@gmail.com)
public final class ReadUtils {
    public static function skip(input:IDataInput, wireType:uint):void {
        switch (wireType) {
            case WireType.VARINT:
                while (input.readUnsignedByte() >= 0x80) {
                }
                break;
            case WireType.FIXED_64_BIT:
                input.readInt();
                input.readInt();
                break;
            case WireType.LENGTH_DELIMITED:
                const length:uint = readUInt32(input);
                for (var i:uint = 0; i < length; i++) {
                    input.readByte()
                }
                break;
            case WireType.FIXED_32_BIT:
                input.readInt();
                break;
            default:
                throw new IOError("Invalid wire type: " + wireType)
        }
    }

    public static function readDouble(input:IDataInput):Number {
        return input.readDouble();
    }

    public static function readFloat(input:IDataInput):Number {
        return input.readFloat();
    }

    public static function readInt64AsNumber(input:IDataInput):Number {
        return readInt64(input).toNumber();
    }

    public static function readInt64(input:IDataInput):Int64 {
        const result:Int64 = new Int64();
        var b:uint;
        var i:uint = 0;
        for (; ; i += 7) {
            b = input.readUnsignedByte();
            if (i == 28) {
                break;
            } else {
                if (b >= 0x80) {
                    result.low |= ((b & 0x7f) << i);
                } else {
                    result.low |= (b << i);
                    return result;
                }
            }
        }
        if (b >= 0x80) {
            b &= 0x7f;
            result.low |= (b << i);
            result.high = b >>> 4;
        } else {
            result.low |= (b << i);
            result.high = b >>> 4;
            return result;
        }
        for (i = 3; ; i += 7) {
            b = input.readUnsignedByte();
            if (i < 32) {
                if (b >= 0x80) {
                    result.high |= ((b & 0x7f) << i);
                } else {
                    result.high |= (b << i);
                    break;
                }
            }
        }
        return result;
    }

    public static function readUInt64AsNumber(input:IDataInput):Number {
        return readUInt64(input).toNumber();
    }

    public static function readUInt64(input:IDataInput):UInt64 {
        const result:UInt64 = new UInt64();
        var b:uint;
        var i:uint = 0;
        for (; ; i += 7) {
            b = input.readUnsignedByte();
            if (i == 28) {
                break;
            } else {
                if (b >= 0x80) {
                    result.low |= ((b & 0x7f) << i);
                } else {
                    result.low |= (b << i);
                    return result;
                }
            }
        }
        if (b >= 0x80) {
            b &= 0x7f;
            result.low |= (b << i);
            result.high = b >>> 4;
        } else {
            result.low |= (b << i);
            result.high = b >>> 4;
            return result;
        }
        for (i = 3; ; i += 7) {
            b = input.readUnsignedByte();
            if (i < 32) {
                if (b >= 0x80) {
                    result.high |= ((b & 0x7f) << i);
                } else {
                    result.high |= (b << i);
                    break;
                }
            }
        }
        return result;
    }

    public static function readInt32(input:IDataInput):int {
        return int(readUInt32(input));
    }

    public static function readFixed64AsNumber(input:IDataInput):Number {
        return readFixed64(input).toNumber();
    }

    public static function readFixed64(input:IDataInput):UInt64 {
        const result:UInt64 = new UInt64();
        result.low = input.readUnsignedInt();
        result.high = input.readUnsignedInt();
        return result
    }

    public static function readFixed32(input:IDataInput):uint {
        return input.readUnsignedInt();
    }

    public static function readBool(input:IDataInput):Boolean {
        return readUInt32(input) != 0;
    }

    public static function readString(input:IDataInput):String {
        const length:uint = readUInt32(input);
        return input.readUTFBytes(length);
    }

    public static function readBytes(input:IDataInput):ByteArray {
        const result:ByteArray = new ByteArray();
        const length:uint = readUInt32(input);
        if (length > 0) {
            input.readBytes(result, 0, length);
        }
        return result;
    }

    public static function readUInt32(input:IDataInput):uint {
        var result:uint = 0;
        for (var i:uint = 0; ; i += 7) {
            const b:uint = input.readUnsignedByte();
            if (i < 32) {
                if (b >= 0x80) {
                    result |= ((b & 0x7f) << i);
                } else {
                    result |= (b << i);
                    break;
                }
            } else {
                while (input.readUnsignedByte() >= 0x80) {
                }
                break;
            }
        }
        return result;
    }

    public static function readEnumId(input:IDataInput):int {
        return readInt32(input);
    }

    public static function readSFixed32(input:IDataInput):int {
        return input.readInt();
    }

    public static function readSFixed64AsNumber(input:IDataInput):Number {
        return readSFixed64(input).toNumber();
    }

    public static function readSFixed64(input:IDataInput):Int64 {
        const result:Int64 = new Int64();
        result.low = input.readUnsignedInt();
        result.high = input.readInt();
        return result;
    }

    public static function readSInt32(input:IDataInput):int {
        return ZigZag.decode32(readUInt32(input));
    }

    public static function readSInt64AsNumber(input:IDataInput):Number {
        return readSInt64(input).toNumber();
    }

    public static function readSInt64(input:IDataInput):Int64 {
        const result:Int64 = readInt64(input);
        const low:uint = result.low;
        const high:uint = result.high;
        result.low = ZigZag.decode64low(low, high);
        result.high = ZigZag.decode64high(low, high);
        return result;
    }
}
}