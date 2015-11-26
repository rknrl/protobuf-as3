//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.protobuf {
public class Int64 {
    public static const INT_RANGE:Number = int.MAX_VALUE - int.MIN_VALUE + 1;

    public var low:uint;
    public var high:int;

    public function Int64(low:uint = 0, high:int = 0) {
        this.low = low;
        this.high = high;
    }

    public static function fromNumber(n:Number):Int64 {
        return new Int64(n, Math.floor(n / INT_RANGE))
    }

    public final function toNumber():Number {
        return high * INT_RANGE + low
    }
}
}
