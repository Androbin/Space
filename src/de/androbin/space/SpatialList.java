package de.androbin.space;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.*;
import java.util.stream.*;
import de.androbin.util.*;

public final class SpatialList<T> implements Space<T> {
  private final List<Pair<T, Rectangle>> map = new ArrayList<>();
  
  @ Override
  public void add( final T object, final Rectangle bounds ) {
    map.add( new Pair<>( object, bounds ) );
  }
  
  @ Override
  public Stream<T> filter( final Predicate<Rectangle> test ) {
    return map.stream()
        .filter( pair -> test.test( pair.second ) )
        .map( pair -> pair.first );
  }
  
  @ Override
  public void remove( final T object, final Rectangle window ) {
    map.removeIf( pair -> pair.first.equals( object ) && pair.second.contains( window ) );
  }
  
  @ Override
  public void set( final T object, final Rectangle window, final Rectangle bounds ) {
    final Pair<T, Rectangle> entry = map.stream()
        .filter( pair -> pair.first.equals( object ) && pair.second.contains( window ) )
        .findAny().orElse( null );
    
    if ( entry == null ) {
      throw new NoSuchElementException();
    }
    
    entry.second = bounds;
  }
}