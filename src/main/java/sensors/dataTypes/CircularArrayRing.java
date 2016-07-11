package sensors.dataTypes;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * RPITank
 * Created by MAWood on 11/07/2016.
 */
public class CircularArrayRing<T> extends AbstractCollection<T>
{

    private Object[] data;
    private int size;
    private int head;

    public CircularArrayRing(int i) {
        data = new Object[i];
        size = 0;
        head = 0;
    }
    public CircularArrayRing() {
        this(10);
    }

    @Override
    public Iterator<T> iterator() {
        return new CircularArrayRingIterator<>(this);
    }

    @Override
    public int size() {
        return size;
    }

    public T get(int index) throws IndexOutOfBoundsException {
        if(index >= size) throw new IndexOutOfBoundsException();
        if(index < 0)    throw new IndexOutOfBoundsException();
        int pointer = head - index;
        if (pointer < 0) pointer+= size;
        return (T) data[pointer];
    }


    @Override
    public boolean add(T item) {
        head = (head+1)%data.length;
        data[head] = item;
        if(size < data.length) size++;
        return true;
    }

    private class CircularArrayRingIterator<T> implements Iterator<T> {

        CircularArrayRing<T> ring;
        int current;

        public CircularArrayRingIterator(CircularArrayRing<T> ring)
        {
            this.ring = ring;
            current = 0;
        }

        @Override
        public boolean hasNext() {
            return (current + 1< ring.size());
        }

        @Override
        public T next() {
            if(!this.hasNext()) throw new NoSuchElementException();
            current++;
            return ring.get(current - 1);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}

