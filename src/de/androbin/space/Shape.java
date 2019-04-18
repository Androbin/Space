package de.androbin.space;

import de.androbin.space.Shape;
import java.awt.*;

public interface Shape {
  boolean contains( Point p );
  
  Dimension getSize();
  
  static Shape rect( final Dimension size ) {
    return new Shape() {
      @ Override
      public boolean contains( final Point p ) {
        return p.x >= 0 && p.x < size.width
            && p.y >= 0 && p.y < size.height;
      }
      
      @ Override
      public Dimension getSize() {
        return size;
      }
    };
  }
}