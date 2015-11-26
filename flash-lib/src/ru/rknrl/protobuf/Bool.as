//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.protobuf {
public class Bool {
    public static const TRUE:Bool = new Bool(true);
    public static const FALSE:Bool = new Bool(false);

    private var _value:Boolean;

    public function get value():Boolean {
        return _value;
    }

    public function Bool(value:Boolean) {
        _value = value;
    }

    public function toString():String {
        return _value.toString();
    }
}
}
