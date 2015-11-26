//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.protobuf {
public class UInt64 {
    public var low:uint;
    public var high:uint;

    public function UInt64(low:uint = 0, high:uint = 0) {
        this.low = low;
        this.high = high;
    }

    public static function fromNumber(n:Number):UInt64 {
        return new UInt64(n, Math.floor(n / Int64.INT_RANGE))
    }

    public final function toNumber():Number {
        return high * Int64.INT_RANGE + low
    }
}
}
