package com.netease.protocGenAs3;

import java.util.HashMap;

final class Scope<Proto> {

    // 如果 proto instanceOf Scope ，则这个 Scope 对另一 Scope 的引用
    public final String fullName;
    public final Scope<?> parent;
    public final Proto proto;
    public final boolean export;
    public final HashMap<String, Scope<?>> children = new HashMap<String, Scope<?>>();

    private Scope<?> find(String[] pathElements, int i) {
        Scope<?> result = this;
        for (; i < pathElements.length; i++ ) {
            String name = pathElements[ i ];
            if ( result.children.containsKey( name ) ) {
                result = result.children.get( name );
            } else {
                return null;
            }
        }
        while ( result.proto instanceof Scope) {
            result = (Scope<?>) result.proto;
        }
        return result;
    }

    private Scope<?> getRoot() {
        Scope<?> scope = this;
        while ( scope.parent != null ) {
            scope = scope.parent;
        }
        return scope;
    }

    public Scope<?> find(String path) {
        String[] pathElements = path.split( "\\." );
        if ( pathElements[ 0 ].equals( "" ) ) {
            return getRoot().find( pathElements, 1 );
        } else {
            for ( Scope<?> scope = this; scope != null; scope = scope.parent ) {
                Scope<?> result = scope.find( pathElements, 0 );
                if ( result != null ) {
                    return result;
                }
            }
            return null;
        }
    }

    private Scope<?> findOrCreate(String[] pathElements, int i) {
        Scope<?> scope = this;
        for (; i < pathElements.length; i++ ) {
            String name = pathElements[ i ];
            if ( scope.children.containsKey( name ) ) {
                scope = scope.children.get( name );
            } else {
                Scope<Object> child =
                        new Scope<Object>( scope, null, false, name );
                scope.children.put( name, child );
                scope = child;
            }
        }
        return scope;
    }

    public Scope<?> findOrCreate(String path) {
        String[] pathElements = path.split( "\\." );
        if ( pathElements[ 0 ].equals( "" ) ) {
            return getRoot().findOrCreate( pathElements, 1 );
        } else {
            return findOrCreate( pathElements, 0 );
        }
    }

    Scope(Scope<?> parent, Proto proto, boolean export,
          String name) {
        this.parent = parent;
        this.proto = proto;
        this.export = export;
        if ( parent == null || parent.fullName == null ||
                parent.fullName.equals( "" ) ) {
            fullName = name;
        } else {
            fullName = parent.fullName + '.' + name;
        }
    }

    public <ChildProto> Scope<ChildProto> addChild(
            String name, ChildProto proto, boolean export) {
        assert (name != null);
        assert (!name.equals( "" ));
        Scope<ChildProto> child =
                new Scope<ChildProto>( this, proto, export, name );
        if ( children.containsKey( child ) ) {
            throw new IllegalArgumentException();
        }
        children.put( name, child );
        return child;
    }

    public static Scope<Object> root() {
        return new Scope<Object>( null, null, false, "" );
    }
}
