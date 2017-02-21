package it.unimi.dsi.sux4j.mph;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import it.unimi.dsi.bits.TransformationStrategies;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLists;
import it.unimi.dsi.sux4j.io.ChunkedHashStore;

public class GOV3FunctionTest {


	private void check( int size, String[] s, GOV3Function<CharSequence> mph, int signatureWidth ) {
		if ( signatureWidth < 0 ) for ( int i = s.length; i-- != 0; ) assertEquals( 1, mph.getLong( s[ i ] ) );
		else for ( int i = s.length; i-- != 0; ) assertEquals( i, mph.getLong( s[ i ] ) );

		// Exercise code for negative results
		if ( signatureWidth == 0 ) for ( int i = size; i-- != 0; ) mph.getLong( Integer.toString( i + size ) );
		else if ( signatureWidth < 0 ) for ( int i = size; i-- != 0; ) assertEquals( 0, mph.getLong( Integer.toString( i + size ) ) );
		else for ( int i = size; i-- != 0; ) assertEquals( -1, mph.getLong( Integer.toString( i + size ) ) );
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNumbers() throws IOException, ClassNotFoundException {
		for ( int outputWidth = 20; outputWidth < Long.SIZE; outputWidth += 8 ) {
			for ( int signatureWidth: new int[] { -32, 0, 32, 64 } ) {
				for ( int size : new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 64, 87, 88, 89, 90, 91, 92, 93, 100, 1000, 10000, 100000 } ) {
					String[] s = new String[ size ];
					for ( int i = s.length; i-- != 0; )
						s[ i ] = Integer.toString( i );

					GOV3Function<CharSequence> mph = new GOV3Function.Builder<CharSequence>().keys( Arrays.asList( s ) ).transform( TransformationStrategies.utf16() ).signed( signatureWidth ).build();

					check( size, s, mph, signatureWidth );

					File temp = File.createTempFile( getClass().getSimpleName(), "test" );
					temp.deleteOnExit();
					BinIO.storeObject( mph, temp );
					mph = (GOV3Function<CharSequence>)BinIO.loadObject( temp );

					check( size, s, mph, signatureWidth );
					
					// From store
					ChunkedHashStore<CharSequence> chunkedHashStore = new ChunkedHashStore<CharSequence>( TransformationStrategies.utf16(), null, signatureWidth < 0 ? -signatureWidth : 0, null );
					chunkedHashStore.addAll( Arrays.asList( s ).iterator() );
					chunkedHashStore.checkAndRetry( Arrays.asList( s ) );
					mph = new GOV3Function.Builder<CharSequence>().store( chunkedHashStore ).signed( signatureWidth ).build();
					chunkedHashStore.close();

					check( size, s, mph, signatureWidth );
				}
			}
		}
	}

	@Test
	public void testLongNumbers() throws IOException {
		LongArrayList l = new LongArrayList( new long[] { 0x234904309830498L, 0xae049345e9eeeeeL, 0x23445234959234L, 0x239234eaeaeaeL } );
		GOV3Function<CharSequence> mph = new GOV3Function.Builder<CharSequence>().keys( Arrays.asList( new String[] { "a", "b", "c", "d" } ) ).transform( TransformationStrategies.utf16() ).values( l ).build();
		assertEquals( l.getLong( 0 ), mph.getLong( "a" ) );
		assertEquals( l.getLong( 1 ), mph.getLong( "b" ) );
		assertEquals( l.getLong( 2 ), mph.getLong( "c" ) );
		assertEquals( l.getLong( 3 ), mph.getLong( "d" ) );
		mph = new GOV3Function.Builder<CharSequence>().keys( Arrays.asList( new String[] { "a", "b", "c", "d" } ) ).transform( TransformationStrategies.utf16() ).values( l, Long.SIZE ).build();
		assertEquals( l.getLong( 0 ), mph.getLong( "a" ) );
		assertEquals( l.getLong( 1 ), mph.getLong( "b" ) );
		assertEquals( l.getLong( 2 ), mph.getLong( "c" ) );
		assertEquals( l.getLong( 3 ), mph.getLong( "d" ) );
		mph = new GOV3Function.Builder<CharSequence>().keys( Arrays.asList( new String[] { "a", "b", "c", "d" } ) ).transform( TransformationStrategies.utf16() ).values( l, Long.SIZE ).indirect().build();
		assertEquals( l.getLong( 0 ), mph.getLong( "a" ) );
		assertEquals( l.getLong( 1 ), mph.getLong( "b" ) );
		assertEquals( l.getLong( 2 ), mph.getLong( "c" ) );
		assertEquals( l.getLong( 3 ), mph.getLong( "d" ) );
	}

	@Test
	public void testDictionary() throws IOException {
		GOV3Function<CharSequence> mph = new GOV3Function.Builder<CharSequence>().keys( Arrays.asList( new String[] { "a", "b", "c", "d" } ) ).transform( TransformationStrategies.utf16() ).dictionary( 8 ).build();
		assertEquals( 1, mph.getLong( "a" ) );
		assertEquals( 1, mph.getLong( "b" ) );
		assertEquals( 1, mph.getLong( "c" ) );
		assertEquals( 1, mph.getLong( "d" ) );
		assertEquals( 0, mph.getLong( "e" ) );
	}

	@Test
	public void testFakeDuplicates() throws IOException {
		GOV3Function<String> mph = new GOV3Function.Builder<String>().keys(
				new Iterable<String>() {
					int iteration;

					public Iterator<String> iterator() {
						if ( iteration++ > 2 ) return Arrays.asList( new String[] { "a", "b", "c" } ).iterator();
						return Arrays.asList( new String[] { "a", "b", "a" } ).iterator();
					}
				} ).transform( TransformationStrategies.utf16() ).build();
		assertEquals( 0, mph.getLong( "a" ) );
		assertEquals( 1, mph.getLong( "b" ) );
		assertEquals( 2, mph.getLong( "c" ) );
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testRealDuplicates() throws IOException {
		new GOV3Function.Builder<String>().keys(
				new Iterable<String>() {
					public Iterator<String> iterator() {
						return Arrays.asList( new String[] { "a", "b", "a" } ).iterator();
					}
				} ).transform( TransformationStrategies.utf16() ).build();
	}

	@Test
	public void testEmpty() throws IOException {
		List<String> emptyList = Collections.emptyList();
		GOV3Function<String> mph = new GOV3Function.Builder<String>().keys( emptyList ).transform( TransformationStrategies.utf16() ).build();
		assertEquals( -1, mph.getLong( "a" ) );
		mph = new GOV3Function.Builder<String>().keys( emptyList ).dictionary( 10 ).transform( TransformationStrategies.utf16() ).build();
		assertEquals( 0, mph.getLong( "a" ) );
		mph = new GOV3Function.Builder<String>().keys( emptyList ).values( LongLists.EMPTY_LIST, 10 ).transform( TransformationStrategies.utf16() ).build();
		assertEquals( -1, mph.getLong( "a" ) );
		
	}
}
