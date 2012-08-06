/**
 * worldmap - an extension to JMapViewer which provides additional
 *            functionality. New functions allow setting markers,
 *            adding layers, and printing tracks on the map. (see
 *            http://wiki.openstreetmap.org/wiki/JMapViewer for more
 *            information on JMapViewer)
 *
 * Copyright (c) 2011
 *
 * Fraunhofer FOKUS
 * www.fokus.fraunhofer.de
 *
 * in cooperation with
 *
 * Technical University Berlin
 * www.av.tu-berlin.de
 *
 * Ramon Masek <ramon.masek@fokus.fraunhofer.de>
 * Christian Henke <c.henke@tu-berlin.de>
 * Carsten Schmoll <carsten.schmoll@fokus.fraunhofer.de>
 * Julian Vetter <julian.vetter@fokus.fraunhofer.de>
 * Jens Krenzin <jens.krenzin@fokus.fraunhofer.de>
 * Michael Gehring <michael.gehring@fokus.fraunhofer.de>
 * Tacio Grespan Santos
 * Fabian Wolff
 *
 * For questions/comments contact packettracking@fokus.fraunhofer.de
 *
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses/>.
 */

package de.fhg.fokus.net.worldmap.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A event support with a user supplied executor (PropertyChangeSupport relies on
 * AWT-EventQueue which we should avoid to used in case we are not dealing with
 * ui drawing) 
 * <pre>
 * E - event type (enum)
 * O - notification type (class)
 * </pre>
 *
 */
public final class EventSupport<E,O> {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	public static interface EventListener<O> {
		public void onEvent( O e );
	}
	public static interface DispatchFuture {
		/**
		 * Await all events dispatched.
		 * 
		 */
		public void await();
	}
	private final Map<E, List<EventListener<O>>> eventLsnMap = new ConcurrentHashMap<E, List<EventListener<O>>>();
	private final Map<EventListener<O>, E> lsnTypeMap = new ConcurrentHashMap<EventListener<O>, E>();
	
	private final ExecutorService executor;
	public EventSupport(ExecutorService executor) {
		super();
		this.executor = executor;
	}
	/**
	 * Add event listener. In case evt or lsn were null it does nothing.
	 * 
	 * @param evt
	 * @param lsn
	 */
	public void addEventListener( final E evt, final EventListener<O> lsn ){
		executor.execute(new Runnable() {
			@Override
			public void run() {
				if(evt==null||lsn==null){
					return;
				}
				List<EventListener<O>> lsnList = eventLsnMap.get(evt);
				if( lsnList==null){
					// event not yet registered, allocate a list storing listeners
					lsnList = new CopyOnWriteArrayList<EventListener<O>>();
					eventLsnMap.put(evt, lsnList);
				}
				lsnTypeMap.put(lsn, evt);
				if(!lsnList.contains(lsn)){
				  lsnList.add(lsn);
				} else {
					logger.warn("Listener {} for event {} already added, ignoring it.",lsn.hashCode(),evt);
				}
			}
		});
	}
	/**
	 * Remove event listener. If it's already removed it does nothing.
	 * It has O(n) (time) for each event type. In case you have lots of
	 * event listeners of same type, please consider using
	 * removeAllListeners( E evt )
	 * 
	 * @param lsn Listener, in case of null it does nothing
	 */
	public void removeEventListener( EventListener<O> lsn ){
		if(lsn==null){
			return;
		}
		E evt = lsnTypeMap.get(lsn);
		if( evt==null){
			// probably it were already removed, simply return
			return;
		}
		List<EventListener<O>> lsnList = eventLsnMap.get(evt);
		if( lsnList == null){
			return;
		}
		lsnList.remove(lsn);
		// deallocate maps in case no more event of this type exist
		if(lsnList.size()==0){
			eventLsnMap.remove(lsnList);
			lsnTypeMap.remove(evt);
		}
	}
	/**
	 * Remove all listeners of input event type
	 * 
	 * @param evt
	 */
	public void removeAllListeners( E evt){
		List<EventListener<O>> lsnList = eventLsnMap.get(evt);
		if( lsnList == null){
			return;
		}
		lsnList.clear();
		eventLsnMap.remove(lsnList);
		lsnTypeMap.remove(evt);
	}
	/**
	 * Remove all listeners, i.e. reset event support
	 */
	public void removeAllListeners(){
		eventLsnMap.clear();
		lsnTypeMap.clear();
	}
	/**
	 * Dispatch events
	 * 
	 * @param evt
	 * @param obj
	 */
	public void dispatch( E evt, final O obj){
		final List<EventListener<O>> lsnList = eventLsnMap.get(evt);
		if( lsnList==null){
			return;
		}
		executor.execute( new Runnable() {
			@Override
			public void run() {
				for( EventListener<O> lsn: lsnList ){
					lsn.onEvent(obj);
				}
			}
		});
	}
	/**
	 * Dispatch event passing a notification object. It uses one thread per
	 * event type. The user (the one who adds event listeners) is responsible
	 * for creating new threads if needed.
	 * 
	 * @param evt
	 * @param obj
	 * @return DispatchFuture
	 * 
	 */
	public DispatchFuture dispatchFuture(final E evt, final O obj ){
		final BlockingQueue<Object> await = new SynchronousQueue<Object>();
		DispatchFuture df = new DispatchFuture() {
			@Override
			public void await() {
				if( eventLsnMap.get(evt)==null){
					// there's no one awaiting for this event
					return;
				}
				try {
					await.put(this);
				} catch (InterruptedException e) {
					logger.debug(e.getMessage());
				}
			}
		};
		final List<EventListener<O>> lsnList = eventLsnMap.get(evt);
		if( lsnList==null){
			await.poll();
			return df;
		}
		executor.execute( new Runnable() {
			@Override
			public void run() {
				for( EventListener<O> lsn: lsnList ){
					lsn.onEvent(obj);
				}
				await.poll();
			}
		});
		return df;
	}
}
