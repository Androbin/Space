package de.androbin.space;

import java.awt.*;
import java.util.stream.*;

public interface Space<T> {
  void add( T object, Rectangle bounds );
  
  Stream<T> filter( Rectangle window );
  
  Stream<T> filter( Point pos );
  
  void remove( T object, Rectangle window );
  
  default void set( final T object, final Rectangle window, final Rectangle bounds ) {
    remove( object, window );
    add( object, bounds );
  };
}