//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package com.netease.protobuf {
public final class UInt64 {
    private static const MAX_UINT:Number = Number(int.MAX_VALUE * 2) + 2;

    public var low:uint;
    public var high:uint;

    public function UInt64(low:uint = 0, high:uint = 0) {
        this.low = low;
        this.high = high;
    }

    public static function fromNumber(n:Number):UInt64 {
        if (n < 0) throw new Error();
        var i:UInt64 = new UInt64();
        i.high = n / MAX_UINT;
        i.low = n % MAX_UINT;
        return i;
    }

    public function toNumber():Number {
        return low + high * MAX_UINT;
    }
}
}
