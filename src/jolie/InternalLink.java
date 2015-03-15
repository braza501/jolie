/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/


package jolie;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import jolie.net.CommMessage;
import jolie.process.InputProcess;
import jolie.process.Process;


/** Internal synchronization link for parallel processes.
 * 
 * @author Fabrizio Montesi
 */
public class InternalLink extends AbstractMappedGlobalObject implements InputHandler
{
	private static HashMap< String, InternalLink > idMap = 
		new HashMap< String, InternalLink >();
	
	private LinkedList< InputProcess > procsList;
	private LinkedList< Process > outList;
	
	public InternalLink( String id )
	{
		super( id );
		procsList = new LinkedList< InputProcess >();
		outList = new LinkedList< Process >();
	}
	
	public synchronized void signForMessage( InputProcess process )
	{
		if ( outList.isEmpty() )
			procsList.addFirst( process );
		else {
			Process outProc = outList.removeLast();
			synchronized( outProc ) {
				outProc.notify();
			}
			process.recvMessage( new CommMessage( id() ) );
		}
	}
	
	public synchronized void cancelWaiting( InputProcess process ) 
	{
		procsList.remove( process );
	}

	public void linkIn( InputProcess process )
	{
		Process outProc = null;
		synchronized( this ) {
			if ( outList.isEmpty() ) {
				procsList.addFirst( process );
			} else
				outProc = outList.getLast();
		}

		if ( outProc == null ) {
			synchronized( process ) {
				try {
					process.wait();
				} catch( InterruptedException e ) {}
			}
		} else {
			synchronized( outProc ) {
				outProc.notify();
			}
		}
	}
	
	public void linkOut( Process process )
	{
		InputProcess inProc = null;
		synchronized( this ) {
			if ( procsList.isEmpty() ) {
				outList.addFirst( process );
			} else
				inProc = procsList.getLast();
		}

		if ( inProc == null ) {
			synchronized( process ) {
				try {
					process.wait();
				} catch( InterruptedException e ) {}
			}
		} else {
			inProc.recvMessage( new CommMessage( id() ) );
		}
	}
	
	public static InternalLink getById( String id )
		throws InvalidIdException
	{
		InternalLink retVal = idMap.get( id );
		if ( retVal == null )
			throw new InvalidIdException( id );

		return retVal;
	}
	
	public final void register()
	{
		idMap.put( id(), this );
	}
	
	public static Collection< InternalLink > getAll()
	{
		return idMap.values();
	}
}