/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.util;

import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.io.IOException;
import java.util.function.Function;

/**
 * <p>Hash table and linked list implementation of the {@code Map} interface,
 * with well-defined encounter order.  This implementation differs from
 * {@code HashMap} in that it maintains a doubly-linked list running through all of
 * its entries.  This linked list defines the encounter order (the order of iteration),
 * which is normally the order in which keys were inserted into the map
 * (<i>insertion-order</i>). The least recently inserted entry (the eldest) is
 * first, and the youngest entry is last. Note that encounter order is not affected
 * if a key is <i>re-inserted</i> into the map with the {@code put} method. (A key
 * {@code k} is reinserted into a map {@code m} if {@code m.put(k, v)} is invoked when
 * {@code m.containsKey(k)} would return {@code true} immediately prior to
 * the invocation.) The reverse-ordered view of this map is in the opposite order, with
 * the youngest entry appearing first and the eldest entry appearing last.
 * The encounter order of entries already in the map can be changed by using
 * the {@link #putFirst putFirst} and {@link #putLast putLast} methods.
 *
 * <p>This implementation spares its clients from the unspecified, generally
 * chaotic ordering provided by {@link HashMap} (and {@link Hashtable}),
 * without incurring the increased cost associated with {@link TreeMap}.  It
 * can be used to produce a copy of a map that has the same order as the
 * original, regardless of the original map's implementation:
 * <pre>{@code
 *     void foo(Map<String, Integer> m) {
 *         Map<String, Integer> copy = new LinkedHashMap<>(m);
 *         ...
 *     }
 * }</pre>
 * This technique is particularly useful if a module takes a map on input,
 * copies it, and later returns results whose order is determined by that of
 * the copy.  (Clients generally appreciate having things returned in the same
 * order they were presented.)
 *
 * <p>A special {@link #LinkedHashMap(int,float,boolean) constructor} is
 * provided to create a linked hash map whose encounter order is the order
 * in which its entries were last accessed, from least-recently accessed to
 * most-recently (<i>access-order</i>).  This kind of map is well-suited to
 * building LRU caches.  Invoking the {@code put}, {@code putIfAbsent},
 * {@code get}, {@code getOrDefault}, {@code compute}, {@code computeIfAbsent},
 * {@code computeIfPresent}, or {@code merge} methods results
 * in an access to the corresponding entry (assuming it exists after the
 * invocation completes). The {@code replace} methods only result in an access
 * of the entry if the value is replaced.  The {@code putAll} method generates one
 * entry access for each mapping in the specified map, in the order that
 * key-value mappings are provided by the specified map's entry set iterator.
 * <i>No other methods generate entry accesses.</i> Invoking these methods on the
 * reversed view generates accesses to entries on the backing map. Note that in the
 * reversed view, an access to an entry moves it first in encounter order.
 * Explicit-positioning methods such as {@code putFirst} or {@code lastEntry}, whether on
 * the map or on its reverse-ordered view, perform the positioning operation and
 * do not generate entry accesses. Operations on the {@code keySet}, {@code values},
 * and {@code entrySet} views or on their sequenced counterparts do <i>not</i> affect
 * the encounter order of the backing map.
 *
 * <p>The {@link #removeEldestEntry(Map.Entry)} method may be overridden to
 * impose a policy for removing stale mappings automatically when new mappings
 * are added to the map. Alternatively, since the "eldest" entry is the first
 * entry in encounter order, programs can inspect and remove stale mappings through
 * use of the {@link #firstEntry firstEntry} and {@link #pollFirstEntry pollFirstEntry}
 * methods.
 *
 * <p>This class provides all of the optional {@code Map} and {@code SequencedMap} operations,
 * and it permits null elements.  Like {@code HashMap}, it provides constant-time
 * performance for the basic operations ({@code add}, {@code contains} and
 * {@code remove}), assuming the hash function disperses elements
 * properly among the buckets.  Performance is likely to be just slightly
 * below that of {@code HashMap}, due to the added expense of maintaining the
 * linked list, with one exception: Iteration over the collection-views
 * of a {@code LinkedHashMap} requires time proportional to the <i>size</i>
 * of the map, regardless of its capacity.  Iteration over a {@code HashMap}
 * is likely to be more expensive, requiring time proportional to its
 * <i>capacity</i>.
 *
 * <p>A linked hash map has two parameters that affect its performance:
 * <i>initial capacity</i> and <i>load factor</i>.  They are defined precisely
 * as for {@code HashMap}.  Note, however, that the penalty for choosing an
 * excessively high value for initial capacity is less severe for this class
 * than for {@code HashMap}, as iteration times for this class are unaffected
 * by capacity.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a linked hash map concurrently, and at least
 * one of the threads modifies the map structurally, it <em>must</em> be
 * synchronized externally.  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the map.
 *
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:<pre>
 *   Map m = Collections.synchronizedMap(new LinkedHashMap(...));</pre>
 *
 * A structural modification is any operation that adds or deletes one or more
 * mappings or, in the case of access-ordered linked hash maps, affects
 * iteration order.  In insertion-ordered linked hash maps, merely changing
 * the value associated with a key that is already contained in the map is not
 * a structural modification.  <strong>In access-ordered linked hash maps,
 * merely querying the map with {@code get} is a structural modification.
 * </strong>)
 *
 * <p>The iterators returned by the {@code iterator} method of the collections
 * returned by all of this class's collection view methods are
 * <em>fail-fast</em>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * {@code remove} method, the iterator will throw a {@link
 * ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:   <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>The spliterators returned by the spliterator method of the collections
 * returned by all of this class's collection view methods are
 * <em><a href="Spliterator.html#binding">late-binding</a></em>,
 * <em>fail-fast</em>, and additionally report {@link Spliterator#ORDERED}.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @implNote
 * The spliterators returned by the spliterator method of the collections
 * returned by all of this class's collection view methods are created from
 * the iterators of the corresponding collections.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Josh Bloch
 * @see     Object#hashCode()
 * @see     Collection
 * @see     Map
 * @see     HashMap
 * @see     TreeMap
 * @see     Hashtable
 * @since   1.4
 */
public class LinkedHashMap<K,V>
    extends HashMap<K,V>
    implements SequencedMap<K,V>
{

    /*
     * Implementation note.  A previous version of this class was
     * internally structured a little differently. Because superclass
     * HashMap now uses trees for some of its nodes, class
     * LinkedHashMap.Entry is now treated as intermediary node class
     * that can also be converted to tree form. The name of this
     * class, LinkedHashMap.Entry, is confusing in several ways in its
     * current context, but cannot be changed.  Otherwise, even though
     * it is not exported outside this package, some existing source
     * code is known to have relied on a symbol resolution corner case
     * rule in calls to removeEldestEntry that suppressed compilation
     * errors due to ambiguous usages. So, we keep the name to
     * preserve unmodified compilability.
     *
     * The changes in node classes also require using two fields
     * (head, tail) rather than a pointer to a header node to maintain
     * the doubly-linked before/after list. This class also
     * previously used a different style of callback methods upon
     * access, insertion, and removal.
     */

    /**
     * HashMap.Node subclass for normal LinkedHashMap entries.
     */
    static class Entry<K,V> extends HashMap.Node<K,V> {
        Entry<K,V> before, after;
        Entry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 3801124242820219131L;


    /**
     * {@inheritDoc}
     * <p>
     * If this map already contains a mapping for this key, the mapping is relocated if necessary
     * so that it is first in encounter order.
     *
     * @since 21
     */
    public V putFirst(K k, V v) {
        return super.putFirst(k, v);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If this map already contains a mapping for this key, the mapping is relocated if necessary
     * so that it is last in encounter order.
     *
     * @since 21
     */
    public V putLast(K k, V v) {
        return super.putLast(k, v);
    }

    /**
     * Constructs an empty insertion-ordered {@code LinkedHashMap} instance
     * with the specified initial capacity and load factor.
     *
     * @apiNote
     * To create a {@code LinkedHashMap} with an initial capacity that accommodates
     * an expected number of mappings, use {@link #newLinkedHashMap(int) newLinkedHashMap}.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public LinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructs an empty insertion-ordered {@code LinkedHashMap} instance
     * with the specified initial capacity and a default load factor (0.75).
     *
     * @apiNote
     * To create a {@code LinkedHashMap} with an initial capacity that accommodates
     * an expected number of mappings, use {@link #newLinkedHashMap(int) newLinkedHashMap}.
     *
     * @param  initialCapacity the initial capacity
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public LinkedHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Constructs an empty insertion-ordered {@code LinkedHashMap} instance
     * with the default initial capacity (16) and load factor (0.75).
     */
    public LinkedHashMap() {
        super();
    }

    /**
     * Constructs an insertion-ordered {@code LinkedHashMap} instance with
     * the same mappings as the specified map.  The {@code LinkedHashMap}
     * instance is created with a default load factor (0.75) and an initial
     * capacity sufficient to hold the mappings in the specified map.
     *
     * @param  m the map whose mappings are to be placed in this map
     * @throws NullPointerException if the specified map is null
     */
    public LinkedHashMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    /**
     * Constructs an empty {@code LinkedHashMap} instance with the
     * specified initial capacity, load factor and ordering mode.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @param  accessOrder     the ordering mode - {@code true} for
     *         access-order, {@code false} for insertion-order
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public LinkedHashMap(int initialCapacity,
                         float loadFactor,
                         boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
    }


    /**
     * Returns {@code true} if this map should remove its eldest entry.
     * This method is invoked by {@code put} and {@code putAll} after
     * inserting a new entry into the map.  It provides the implementor
     * with the opportunity to remove the eldest entry each time a new one
     * is added.  This is useful if the map represents a cache: it allows
     * the map to reduce memory consumption by deleting stale entries.
     *
     * <p>Sample use: this override will allow the map to grow up to 100
     * entries and then delete the eldest entry each time a new entry is
     * added, maintaining a steady state of 100 entries.
     * <pre>
     *     private static final int MAX_ENTRIES = 100;
     *
     *     protected boolean removeEldestEntry(Map.Entry eldest) {
     *        return size() &gt; MAX_ENTRIES;
     *     }
     * </pre>
     *
     * <p>This method typically does not modify the map in any way,
     * instead allowing the map to modify itself as directed by its
     * return value.  It <i>is</i> permitted for this method to modify
     * the map directly, but if it does so, it <i>must</i> return
     * {@code false} (indicating that the map should not attempt any
     * further modification).  The effects of returning {@code true}
     * after modifying the map from within this method are unspecified.
     *
     * <p>This implementation merely returns {@code false} (so that this
     * map acts like a normal map - the eldest element is never removed).
     *
     * @param    eldest The least recently inserted entry in the map, or if
     *           this is an access-ordered map, the least recently accessed
     *           entry.  This is the entry that will be removed if this
     *           method returns {@code true}.  If the map was empty prior
     *           to the {@code put} or {@code putAll} invocation resulting
     *           in this invocation, this will be the entry that was just
     *           inserted; in other words, if the map contains a single
     *           entry, the eldest entry is also the newest.
     * @return   {@code true} if the eldest entry should be removed
     *           from the map; {@code false} if it should be retained.
     */
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return super.removeEldestEntry(eldest);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned view has the same characteristics as specified for the view
     * returned by the {@link #keySet keySet} method.
     *
     * @return {@inheritDoc}
     * @since 21
     */
    public SequencedSet<K> sequencedKeySet() {
        return super.sequencedKeySet();
    }



    /**
     * {@inheritDoc}
     * <p>
     * The returned view has the same characteristics as specified for the view
     * returned by the {@link #values values} method.
     *
     * @return {@inheritDoc}
     * @since 21
     */
    public SequencedCollection<V> sequencedValues() {
        return super.sequencedValues();
    }



    public SequencedSet<Map.Entry<K, V>> sequencedEntrySet() {
        return super.sequencedEntrySet();
    }


    /**
     * Creates a new, empty, insertion-ordered LinkedHashMap suitable for the expected number of mappings.
     * The returned map uses the default load factor of 0.75, and its initial capacity is
     * generally large enough so that the expected number of mappings can be added
     * without resizing the map.
     *
     * @param numMappings the expected number of mappings
     * @param <K>         the type of keys maintained by the new map
     * @param <V>         the type of mapped values
     * @return the newly created map
     * @throws IllegalArgumentException if numMappings is negative
     * @since 19
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int numMappings) {
        if (numMappings < 0) {
            throw new IllegalArgumentException("Negative number of mappings: " + numMappings);
        }
        return new LinkedHashMap<>(HashMap.calculateHashMapCapacity(numMappings));
    }

    // Reversed View

    /**
     * {@inheritDoc}
     * <p>
     * Modifications to the reversed view and its map views are permitted and will be
     * propagated to this map. In addition, modifications to this map will be visible
     * in the reversed view and its map views.
     *
     * @return {@inheritDoc}
     * @since 21
     */
    public SequencedMap<K, V> reversed() {
        return new ReversedLinkedHashMapView<>(this);
    }

    static class ReversedLinkedHashMapView<K, V> extends AbstractMap<K, V>
                                                 implements SequencedMap<K, V> {
        final LinkedHashMap<K, V> base;

        ReversedLinkedHashMapView(LinkedHashMap<K, V> lhm) {
            base = lhm;
        }

        // Object
        // inherit toString() from AbstractMap; it depends on entrySet()

        public boolean equals(Object o) {
            return base.equals(o);
        }

        public int hashCode() {
            return base.hashCode();
        }

        // Map

        public int size() {
            return base.size();
        }

        public boolean isEmpty() {
            return base.isEmpty();
        }

        public boolean containsKey(Object key) {
            return base.containsKey(key);
        }

        public boolean containsValue(Object value) {
            return base.containsValue(value);
        }

        public V get(Object key) {
            return base.get(key);
        }

        public V put(K key, V value) {
            return base.put(key, value);
        }

        public V remove(Object key) {
            return base.remove(key);
        }

        public void putAll(Map<? extends K, ? extends V> m) {
            base.putAll(m);
        }

        public void clear() {
            base.clear();
        }

        public Set<K> keySet() {
            return base.sequencedKeySet().reversed();
        }

        public Collection<V> values() {
            return base.sequencedValues().reversed();
        }

        public Set<Entry<K, V>> entrySet() {
            return base.sequencedEntrySet().reversed();
        }

        public V getOrDefault(Object key, V defaultValue) {
            return base.getOrDefault(key, defaultValue);
        }

        public void forEach(BiConsumer<? super K, ? super V> action) {
            if (action == null)
                throw new NullPointerException();
            int mc = base.modCount;
            for (LinkedHashMap.Entry<K,V> e = base.tail; e != null; e = e.before)
                action.accept(e.key, e.value);
            if (base.modCount != mc)
                throw new ConcurrentModificationException();
        }

        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            if (function == null)
                throw new NullPointerException();
            int mc = base.modCount;
            for (LinkedHashMap.Entry<K,V> e = base.tail; e != null; e = e.before)
                e.value = function.apply(e.key, e.value);
            if (base.modCount != mc)
                throw new ConcurrentModificationException();
        }

        public V putIfAbsent(K key, V value) {
            return base.putIfAbsent(key, value);
        }

        public boolean remove(Object key, Object value) {
            return base.remove(key, value);
        }

        public boolean replace(K key, V oldValue, V newValue) {
            return base.replace(key, oldValue, newValue);
        }

        public V replace(K key, V value) {
            return base.replace(key, value);
        }

        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            return base.computeIfAbsent(key, mappingFunction);
        }

        public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return base.computeIfPresent(key, remappingFunction);
        }

        public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return base.compute(key, remappingFunction);
        }

        public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            return base.merge(key, value, remappingFunction);
        }

        // SequencedMap

        public SequencedMap<K, V> reversed() {
            return base;
        }

        public Entry<K, V> firstEntry() {
            return base.lastEntry();
        }

        public Entry<K, V> lastEntry() {
            return base.firstEntry();
        }

        public Entry<K, V> pollFirstEntry() {
            return base.pollLastEntry();
        }

        public Entry<K, V> pollLastEntry() {
            return base.pollFirstEntry();
        }

        public V putFirst(K k, V v) {
            return base.putLast(k, v);
        }

        public V putLast(K k, V v) {
            return base.putFirst(k, v);
        }
    }
}
