package de.androbin.space;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.*;
import java.util.stream.*;

public final class RTree<T> implements Space<T> {
  private final int min;
  private final int max;
  
  private Node<T> root;
  
  public RTree( final int b ) {
    this( (int) Math.ceil( b * 0.4f ), b );
  }
  
  public RTree( final int min, final int max ) {
    assert min >= 2;
    assert max + 1 >= min * 2;
    
    this.min = min;
    this.max = max;
  }
  
  @ Override
  public void add( final T object, final Rectangle bounds ) {
    insert( new LeafNode<>( object, bounds ) );
  }
  
  private static int cost( final Rectangle rect ) {
    return rect.width + rect.height;
  }
  
  private Stream<T> filter( final Predicate<Rectangle> test ) {
    if ( root == null ) {
      return Stream.empty();
    }
    
    final Stream.Builder<T> builder = Stream.builder();
    root.filter( test, builder );
    return builder.build();
  }
  
  @ Override
  public Stream<T> filter( final Rectangle window ) {
    return filter( key -> key.intersects( window ) );
  }
  
  @ Override
  public Stream<T> filter( final Point pos ) {
    return filter( key -> key.contains( pos ) );
  }
  
  private void insert( final Node<T> node ) {
    if ( root == null ) {
      root = new InnerNode<>();
    }
    
    final Node<T> split = root.insert( node, min, max );
    
    if ( split == null ) {
      return;
    }
    
    final Node<T> parent = new InnerNode<>();
    parent.insert( root, min, max );
    parent.insert( split, min, max );
    root = parent;
  }
  
  private static <T> Rectangle mbr( final List<Node<T>> children ) {
    Rectangle mbr = null;
    
    for ( final Node<T> child : children ) {
      if ( mbr == null ) {
        mbr = child.mbr();
      } else {
        mbr = mbr.union( child.mbr() );
      }
    }
    
    return mbr;
  }
  
  @ Override
  public void remove( final T object, final Rectangle window ) {
    if ( root == null ) {
      return;
    }
    
    final List<Node<T>> deleted = new ArrayList<>();
    
    if ( root.delete( object, window, min, deleted ) ) {
      root = null;
    }
    
    Collections.reverse( deleted );
    
    for ( final Node<T> node : deleted ) {
      insert( node );
    }
  }
  
  @ Override
  public void set( final T object, final Rectangle window, final Rectangle bounds ) {
    if ( root == null ) {
      return;
    }
    
    root.set( object, window, bounds );
  }
  
  private static interface Node<T> {
    boolean delete( T object, Rectangle window, int min, List<Node<T>> deleted );
    
    void filter( Predicate<Rectangle> test, Stream.Builder<T> builder );
    
    Node<T> insert( Node<T> node, int min, int max );
    
    int level();
    
    Rectangle mbr();
    
    boolean set( T object, Rectangle window, Rectangle bounds );
  }
  
  private static final class InnerNode<T> implements Node<T> {
    private final List<Node<T>> children;
    private Rectangle mbr;
    
    private int level;
    
    public InnerNode() {
      children = new ArrayList<>();
    }
    
    private InnerNode( final List<Node<T>> children ) {
      this.children = children;
      mbr = RTree.mbr( children );
      level = children.get( 0 ).level() + 1;
    }
    
    private Node<T> chooseSubtree( final Node<T> node ) {
      Node<T> bestChild = null;
      int bestCost = 0;
      
      for ( final Node<T> child : children ) {
        final int cost = RTree.cost( child.mbr().union( node.mbr() ) ) - RTree.cost( child.mbr() );
        
        if ( bestChild == null || cost < bestCost ) {
          bestChild = child;
          bestCost = cost;
        }
      }
      
      return bestChild;
    }
    
    @ Override
    public boolean delete( final T object, final Rectangle window, final int min,
        final List<Node<T>> deleted ) {
      if ( !mbr.contains( window ) ) {
        return false;
      }
      
      for ( final Node<T> child : children ) {
        if ( !child.delete( object, window, min, deleted ) ) {
          continue;
        }
        
        children.remove( child );
        
        if ( children.size() >= min ) {
          return false;
        }
        
        deleted.addAll( children );
        return true;
      }
      
      return false;
    }
    
    @ Override
    public void filter( final Predicate<Rectangle> test, final Stream.Builder<T> builder ) {
      if ( !test.test( mbr ) ) {
        return;
      }
      
      for ( final Node<T> child : children ) {
        child.filter( test, builder );
      }
    }
    
    private Node<T> handleOverflow( final int min, final int max ) {
      if ( children.size() <= max ) {
        return null;
      }
      
      List<Node<T>> bestSplit1 = null;
      List<Node<T>> bestSplit2 = null;
      int bestCost = 0;
      
      for ( int j = 0; j < 4; j++ ) {
        final int j_ = j;
        final Function<Rectangle, Integer> value = rect -> {
          switch ( j_ ) {
            case 0:
              return rect.x;
            case 1:
              return rect.x + rect.width;
            case 2:
              return rect.y;
            case 3:
              return rect.y + rect.height;
          }
          
          throw new InternalError();
        };
        children.sort( ( a, b ) -> Integer.compare(
            value.apply( a.mbr() ), value.apply( b.mbr() ) ) );
        
        for ( int i = min; i <= children.size() - min; i++ ) {
          final List<Node<T>> s1 = children.subList( 0, i );
          final List<Node<T>> s2 = children.subList( i, children.size() );
          
          final int cost = RTree.cost( RTree.mbr( s1 ) ) + RTree.cost( RTree.mbr( s2 ) );
          
          if ( bestSplit1 == null || cost < bestCost ) {
            bestSplit1 = new ArrayList<>( s1 );
            bestSplit2 = new ArrayList<>( s2 );
            bestCost = cost;
          }
        }
      }
      
      children.clear();
      children.addAll( bestSplit1 );
      
      return new InnerNode<>( bestSplit2 );
    }
    
    @ Override
    public Node<T> insert( final Node<T> node, final int min, final int max ) {
      if ( children.isEmpty() ) {
        mbr = node.mbr();
        level = node.level() + 1;
      }
      
      mbr = mbr.union( node.mbr() );
      
      final int c = Integer.compare( node.level(), level() - 1 );
      assert c <= 0;
      
      if ( c == 0 ) {
        children.add( node );
      } else {
        final Node<T> child = chooseSubtree( node );
        final Node<T> split = child.insert( node, min, max );
        
        if ( split == null ) {
          return null;
        }
        
        children.add( split );
      }
      
      return handleOverflow( min, max );
    }
    
    @ Override
    public int level() {
      return level;
    }
    
    @ Override
    public Rectangle mbr() {
      return mbr;
    }
    
    @ Override
    public boolean set( final T object, final Rectangle window, final Rectangle bounds ) {
      if ( !mbr.contains( window ) ) {
        return false;
      }
      
      for ( final Node<T> child : children ) {
        if ( !child.set( object, window, bounds ) ) {
          continue;
        }
        
        mbr = RTree.mbr( children );
        return true;
      }
      
      return false;
    }
  }
  
  private static final class LeafNode<T> implements Node<T> {
    private final T object;
    private Rectangle bounds;
    
    public LeafNode( final T object, final Rectangle bounds ) {
      this.object = object;
      this.bounds = bounds;
    }
    
    @ Override
    public boolean delete( final T object, final Rectangle window, final int min,
        final List<Node<T>> deleted ) {
      return this.object.equals( object ) && bounds.contains( window );
    }
    
    @ Override
    public void filter( final Predicate<Rectangle> test, final Stream.Builder<T> builder ) {
      if ( test.test( bounds ) ) {
        builder.accept( object );
      }
    }
    
    @ Override
    public Node<T> insert( final Node<T> node, final int min, final int max ) {
      throw new InternalError();
    }
    
    @ Override
    public int level() {
      return 0;
    }
    
    @ Override
    public Rectangle mbr() {
      return bounds;
    }
    
    @ Override
    public boolean set( final T object, final Rectangle window, final Rectangle bounds ) {
      if ( !this.object.equals( object ) || !this.bounds.contains( window ) ) {
        return false;
      }
      
      this.bounds = bounds;
      return true;
    }
  }
}