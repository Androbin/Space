package de.androbin.space;

import java.awt.*;
import java.util.function.*;
import java.util.stream.*;

public interface Space<T> {
  void add( T object, Rectangle bounds );
  
  Stream<T> filter( Predicate<Rectangle> test );
  
  default Stream<T> filter( final Rectangle window ) {
    return filter( bounds -> bounds.intersects( window ) );
  }
  
  default Stream<T> filter( final Point pos ) {
    return filter( bounds -> bounds.contains( pos ) );
  }
  
  void remove( T object, Rectangle window );
  
  default void set( final T object, final Rectangle window, final Rectangle bounds ) {
    remove( object, window );
    add( object, bounds );
  };
  
  default Stream<T> stream() {
    return filter( key -> true );
  }
}