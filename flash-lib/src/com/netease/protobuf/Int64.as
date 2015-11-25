//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package com.netease.protobuf {
public final class Int64 {
    private static const MAX_UINT:Number = Number(int.MAX_VALUE * 2) + 2;

    public var low:int;
    public var high:int;

    public function Int64(low:int = 0, high:int = 0) {
        this.low = low;
        this.high = high;
    }

    public static function fromNumber(n:Number):Int64 {
        var i:Int64 = new Int64();
        i.high = n / MAX_UINT;
        i.low = n % MAX_UINT;
        return i;
    }

    public function copy():Int64 {
        return new Int64(low, high);
    }

    public function increment():void {
        low++;
        if (low == 0) {
            high++;
        }
    }

    public function toNumber():Number {
        return low + high * 4294967296.0;
    }

    public function equals(o:Int64):Boolean {
        return low == o.low && high == o.high;
    }

    public function toString():String {
        return "Int64{low=" + String(low) + ",high=" + String(high) + "}";
    }
}
}
