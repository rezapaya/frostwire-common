/*
 * Created on 13-Jul-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros 
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.impl;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManagerFactory;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.global.GlobalMangerProgressListener;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.ipfilter.IpFilterManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.platform.PlatformManagerListener;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentDownloader;
import org.gudy.azureus2.plugins.utils.DelayedTask;
import org.gudy.azureus2.plugins.utils.PowerManagementListener;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.UtilitiesImpl;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.backup.BackupManagerFactory;
import com.aelitis.azureus.core.custom.CustomizationManagerFactory;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTListener;
import com.aelitis.azureus.core.dht.speed.DHTSpeedTester;
import com.aelitis.azureus.core.instancemanager.AZInstanceManager;
import com.aelitis.azureus.core.instancemanager.AZInstanceManagerAdapter;
import com.aelitis.azureus.core.instancemanager.AZInstanceManagerFactory;
import com.aelitis.azureus.core.instancemanager.AZInstanceTracked;
import com.aelitis.azureus.core.nat.NATTraverser;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNetworkInterface;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNetworkInterfaceAddress;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminPropertyChangeListener;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPNetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.udp.UDPNetworkManager;
import com.aelitis.azureus.core.pairing.PairingManagerFactory;
import com.aelitis.azureus.core.peermanager.PeerManager;
import com.aelitis.azureus.core.peermanager.nat.PeerNATTraverser;
import com.aelitis.azureus.core.proxy.AEProxySelectorFactory;
import com.aelitis.azureus.plugins.clientid.ClientIDPlugin;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.azureus.core.speedmanager.SpeedLimitHandler;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.speedmanager.SpeedManagerAdapter;
import com.aelitis.azureus.core.speedmanager.SpeedManagerFactory;
import com.aelitis.azureus.core.update.AzureusRestarterFactory;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.core.vuzefile.VuzeFileProcessor;
import com.aelitis.azureus.launcher.classloading.PrimaryClassloader;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.startstoprules.defaultplugin.DefaultRankCalculator;
import com.aelitis.azureus.plugins.startstoprules.defaultplugin.StartStopRulesDefaultPlugin;
import com.aelitis.azureus.plugins.tracker.dht.DHTTrackerPlugin;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author parg
 *
 */

public class 
AzureusCoreImpl 
	implements 	AzureusCore
{
	private final static LogIDs LOGID = LogIDs.CORE;
	protected static AzureusCore		singleton;
	protected static AEMonitor			class_mon	= new AEMonitor( "AzureusCore:class" );
	
	private static final String DM_ANNOUNCE_KEY	= "AzureusCore:announce_key";
	private static final boolean LOAD_PLUGINS_IN_OTHER_THREAD = true;
	
	/** 
	 * Listeners that will be fired after core has completed initialization
	 */
	static List<AzureusCoreRunningListener> coreRunningListeners = new ArrayList<AzureusCoreRunningListener>(1);
	
	static AEMonitor mon_coreRunningListeners = new AEMonitor("CoreCreationListeners");
	
	public static AzureusCore
	create()
	
		throws AzureusCoreException
	{
		try{
			class_mon.enter();
			
			if ( singleton != null ){
		
				throw( new AzureusCoreException( "Azureus core already instantiated" ));
			}
			
			singleton	= new AzureusCoreImpl();
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	public static boolean
	isCoreAvailable()
	{
		return( singleton != null );
	}

	public static boolean
	isCoreRunning()
	{
		return( singleton != null && singleton.isStarted() );
	}

	public static AzureusCore
	getSingleton()
	
		throws AzureusCoreException
	{
		if ( singleton == null ){
			
			throw( new AzureusCoreException( "core not instantiated"));
		}
		
		return( singleton );
	}	

	private PluginInitializer 	pi;
	private GlobalManager		global_manager;
	private AZInstanceManager	instance_manager;
	private SpeedManager		speed_manager;
	private CryptoManager		crypto_manager;
	private NATTraverser		nat_traverser;
	
	private final long create_time;


	private volatile boolean				started;
	private volatile boolean				stopped;
	private volatile boolean				restarting;
	
	private CopyOnWriteList		listeners				= new CopyOnWriteList();
	private CopyOnWriteList		lifecycle_listeners		= new CopyOnWriteList();
	private List				operation_listeners		= new ArrayList();
	
	private CopyOnWriteList<PowerManagementListener>	power_listeners = new CopyOnWriteList<PowerManagementListener>();
	
	private AESemaphore			stopping_sem	= new AESemaphore( "AzureusCore::stopping" );
	
	private AEMonitor			this_mon		= new AEMonitor( "AzureusCore" );

	private AzureusCoreOperation	initialisation_op = createOperation( AzureusCoreOperation.OP_INITIALISATION );
	
	public static boolean SUPPRESS_CLASSLOADER_ERRORS = false;
	
	private boolean ca_shutdown_computer_after_stop	= false;
	private long	ca_last_time_downloading 		= -1;
	private long	ca_last_time_seeding 			= -1;
	
	private boolean prevent_sleep_remove_trigger	= false;
	
	protected
	AzureusCoreImpl()
	{
		create_time = SystemTime.getCurrentTime();
		
		if(!SUPPRESS_CLASSLOADER_ERRORS && !(this.getClass().getClassLoader() instanceof PrimaryClassloader))
			System.out.println("###\nWarning: Core not instantiated through a PrimaryClassloader, this can lead to restricted functionality or bugs in future versions\n###");
		
		COConfigurationManager.initialise();
		
		MessageText.loadBundle();
		
		AEDiagnostics.startup( COConfigurationManager.getBooleanParameter( "diags.enable.pending.writes", false ));
		
		COConfigurationManager.setParameter( "diags.enable.pending.writes", false );
				
		AEDiagnostics.markDirty();
		
		AETemporaryFileHandler.startup();
    
		AEThread2.setOurThread();
		
			// set up a backwards pointer from config -> app dir so we can derive one from the other. It'll get saved on closedown, no need to do so now
				
		COConfigurationManager.setParameter( "azureus.application.directory", new File( SystemProperties.getApplicationPath()).getAbsolutePath());
		COConfigurationManager.setParameter( "azureus.user.directory", new File( SystemProperties.getUserPath()).getAbsolutePath());
		
		crypto_manager = CryptoManagerFactory.getSingleton();
		
		PlatformManagerFactory.getPlatformManager().addListener(
			new PlatformManagerListener()
			{
				public int
				eventOccurred(
					int		type )
				{
					if ( type == ET_SHUTDOWN ){
						
						if (Logger.isEnabled()){
							Logger.log(new LogEvent(LOGID, "Platform manager requested shutdown"));
						}
						
						COConfigurationManager.save();
						
						requestStop();
						
						return( 0 );
						
					}else if ( type == ET_SUSPEND ){
						
						if (Logger.isEnabled()){
							Logger.log(new LogEvent(LOGID, "Platform manager requested suspend"));
						}
						
						COConfigurationManager.save();
						
					}else if ( type == ET_RESUME ){
		
						if (Logger.isEnabled()){
							Logger.log(new LogEvent(LOGID, "Platform manager requested resume"));
						}
						
						announceAll( true );
					}
					
					return( -1 );
				}
			});
		
			//ensure early initialization
				
		CustomizationManagerFactory.getSingleton().initialize();
		
		AEProxySelectorFactory.getSelector();
		
		NetworkManager.getSingleton();
		
		PeerManager.getSingleton();
		
			// Used to be a plugin, but not any more...
		
		ClientIDPlugin.initialize();
		
		pi = PluginInitializer.getSingleton( this, initialisation_op );
		
		
		instance_manager = 
			AZInstanceManagerFactory.getSingleton( 
				new AZInstanceManagerAdapter()
				{
					public String
					getID()
					{
						return( COConfigurationManager.getStringParameter( "ID", "" ));
					}
					
					public InetAddress 
					getPublicAddress() 
					{	
						return( PluginInitializer.getDefaultInterface().getUtilities().getPublicAddress());
					}
					
					public int[]
					getPorts()
					{
						return( new int[]{
							TCPNetworkManager.getSingleton().getTCPListeningPortNumber(),
							UDPNetworkManager.getSingleton().getUDPListeningPortNumber(),
							UDPNetworkManager.getSingleton().getUDPNonDataListeningPortNumber()});

					}
					public VCPublicAddress
					getVCPublicAddress()
					{
						return(
							new VCPublicAddress()
							{
								private VersionCheckClient vcc = VersionCheckClient.getSingleton();
								
								public String
								getAddress()
								{
									return( vcc.getExternalIpAddress( true, false ));
								}
								
								public long
								getCacheTime()
								{
									return( vcc.getSingleton().getCacheTime( false ));
								}
							});
					}

					public AZInstanceTracked.TrackTarget
					track(
						byte[]		hash )
					{
						List	dms = getGlobalManager().getDownloadManagers();
						
						Iterator	it = dms.iterator();
						
						DownloadManager	matching_dm = null;
						
						try{
							while( it.hasNext()){
								
								DownloadManager	dm = (DownloadManager)it.next();
								
								TOTorrent	torrent = dm.getTorrent();
								
								if ( torrent == null ){
									
									continue;
								}
								
								byte[]	sha1_hash = (byte[])dm.getData( "AZInstanceManager::sha1_hash" );
								
								if ( sha1_hash == null ){			

									sha1_hash	= new SHA1Simple().calculateHash( torrent.getHash());
									
									dm.setData( "AZInstanceManager::sha1_hash", sha1_hash );
								}
								
								if ( Arrays.equals( hash, sha1_hash )){
									
									matching_dm	= dm;
									
									break;
								}
							}
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
						
						if ( matching_dm == null ){
							
							return( null );
						}
						
						if ( !matching_dm.getDownloadState().isPeerSourceEnabled( PEPeerSource.PS_PLUGIN )){
							
							return( null );
						}
						
						int	dm_state = matching_dm.getState();
						
						if ( dm_state == DownloadManager.STATE_ERROR || dm_state == DownloadManager.STATE_STOPPED ){
							
							return( null );
						}
						
						try{
						
							final Object target = DownloadManagerImpl.getDownloadStatic( matching_dm );
						
							final boolean	is_seed = matching_dm.isDownloadComplete(true);
							
							return(
								new AZInstanceTracked.TrackTarget()
								{
									public Object
									getTarget()
									{
										return( target );
									}
									
									public boolean
									isSeed()
									{
										return( is_seed );
									}
								});
							
						}catch( Throwable e ){
							
							return( null );
						}
					}
					
					public DHTPlugin 
					getDHTPlugin()
					{
						PluginInterface pi = getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
						
						if ( pi != null ){
							
							return( (DHTPlugin)pi.getPlugin());
						}
						
						return( null );
					}
					
					public UPnPPlugin 
					getUPnPPlugin() 
					{
						PluginInterface pi = getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );
						
						if ( pi != null ){
							
							return((UPnPPlugin)pi.getPlugin());
						}
						
						return( null );
					}
					
					public void 
					addListener(
						final StateListener listener) 
					{
						AzureusCoreImpl.this.addLifecycleListener(
							new AzureusCoreLifecycleAdapter()
							{
								public void 
								started(
									AzureusCore core) 
								{
									listener.started();
								}
								
								public void
								stopping(
									AzureusCore		core )
								{
									listener.stopped();
								}
							});
					}
				});
		
		if ( COConfigurationManager.getBooleanParameter( "speedmanager.enable", true )){
			
			speed_manager	= 
				SpeedManagerFactory.createSpeedManager( 
						this,
						new SpeedManagerAdapter()
						{
							private static final int UPLOAD_SPEED_ADJUST_MIN_KB_SEC		= 10;
							private static final int DOWNLOAD_SPEED_ADJUST_MIN_KB_SEC	= 300;
							
							private boolean setting_limits;
							
							public int
							getCurrentProtocolUploadSpeed(
								int	average_period )
							{
								if ( global_manager != null ){
									
									GlobalManagerStats stats = global_manager.getStats();
									
									return( stats.getProtocolSendRateNoLAN( average_period ));
									
								}else{
									
									return(0);
								}
							}
							
							public int
							getCurrentDataUploadSpeed(
								int	average_period )
							{
								if ( global_manager != null ){
									
									GlobalManagerStats stats = global_manager.getStats();
									
									return( stats.getDataSendRateNoLAN( average_period ));
									
								}else{
									
									return(0);
								}
							}
	
	                        public int
	                        getCurrentProtocolDownloadSpeed(
	                        	int	average_period )
	                        {
	                            if( global_manager != null ){
	                                GlobalManagerStats stats = global_manager.getStats();
	                                return (stats.getProtocolReceiveRateNoLAN( average_period ) );
	                            }else{
	                                return(0);
	                            }
	                        }
	
	                        public int
	                        getCurrentDataDownloadSpeed(
	                        	int	average_period )
	                        {
	                            if( global_manager != null ){
	                                GlobalManagerStats stats = global_manager.getStats();
	                                return (stats.getDataReceiveRateNoLAN( average_period ) );
	                            }else{
	                                return(0);
	                            }
	                        }
	                        
	                        public int
							getCurrentUploadLimit()
							{
								String key = TransferSpeedValidator.getActiveUploadParameter( global_manager );
								
								int	k_per_second = COConfigurationManager.getIntParameter( key );
								
								int	bytes_per_second;
								
								if ( k_per_second == 0 ){
									
									bytes_per_second = Integer.MAX_VALUE;
									
								}else{
									
									bytes_per_second = k_per_second*1024;
								}
								
								return( bytes_per_second );
							}
							
							public void
							setCurrentUploadLimit(
								int		bytes_per_second )
							{
								if ( bytes_per_second != getCurrentUploadLimit()){
									
									String key = TransferSpeedValidator.getActiveUploadParameter( global_manager );
										
									int	k_per_second;
									
									if ( bytes_per_second == Integer.MAX_VALUE ){
										
										k_per_second	= 0;
										
									}else{
									
										k_per_second = (bytes_per_second+1023)/1024;
									}
									
									if ( k_per_second > 0 ){
									
										k_per_second = Math.max( k_per_second, UPLOAD_SPEED_ADJUST_MIN_KB_SEC );
									}
									
									COConfigurationManager.setParameter( key, k_per_second );
								}
							}
							
							public int
							getCurrentDownloadLimit()
							{
								return( TransferSpeedValidator.getGlobalDownloadRateLimitBytesPerSecond());
							}
							
							public void
							setCurrentDownloadLimit(
								int		bytes_per_second )
							{
								if ( bytes_per_second == Integer.MAX_VALUE ){
									
									bytes_per_second = 0;
								}
								
								if ( bytes_per_second > 0 ){
									
									bytes_per_second = Math.max( bytes_per_second, DOWNLOAD_SPEED_ADJUST_MIN_KB_SEC*1024 );
								}
								
								TransferSpeedValidator.setGlobalDownloadRateLimitBytesPerSecond( bytes_per_second );
							}
							
							public Object
							getLimits()
							{
								String up_key 	= TransferSpeedValidator.getActiveUploadParameter( global_manager );
								String down_key	= TransferSpeedValidator.getDownloadParameter();
								
								return( 
									new Object[]{
										up_key,
										new Integer( COConfigurationManager.getIntParameter( up_key )),
										down_key,
										new Integer( COConfigurationManager.getIntParameter( down_key )),
									});
							}
							
							public void
							setLimits(
								Object		limits,
								boolean		do_up,
								boolean		do_down )
							{
								if ( limits == null ){
									
									return;
								}
								try{
									if ( setting_limits ){
										
										return;
									}
								
									setting_limits	= true;
								
									Object[]	bits = (Object[])limits;
									
									if ( do_up ){
										
										COConfigurationManager.setParameter((String)bits[0], ((Integer)bits[1]).intValue());
									}
									
									if ( do_down ){
										
										COConfigurationManager.setParameter((String)bits[2], ((Integer)bits[3]).intValue());
									}
									
								}finally{
									
									setting_limits	= false;
									
								}
							}
						});
		}
		
		nat_traverser = new NATTraverser( this );
		
		PeerNATTraverser.initialise( this );
		
		BackupManagerFactory.getManager( this );
		
			// one off explicit GC to clear up initialisation mem
		
		SimpleTimer.addEvent(
				"AzureusCore:gc",
				SystemTime.getOffsetTime(60*1000),
				new TimerEventPerformer()
				{
					public void 
					perform(
						TimerEvent event) 
					{
						System.gc();
					}
				});
	}
	
	public long
	getCreateTime()
	{
		return( create_time );
	}
	
	protected void
	announceAll(
		boolean	force )
	{
		Logger.log(	new LogEvent(LOGID, "Updating trackers" ));

		GlobalManager gm = getGlobalManager();
		
		if ( gm != null ){
			
			List	downloads = gm.getDownloadManagers();
			
			long now	= SystemTime.getCurrentTime();

			for (int i=0;i<downloads.size();i++){
				
				DownloadManager	dm = (DownloadManager)downloads.get(i);
				
				Long	last_announce_l = (Long)dm.getUserData( DM_ANNOUNCE_KEY );
				
				long	last_announce	= last_announce_l==null?create_time:last_announce_l.longValue();
				
				TRTrackerAnnouncer an = dm.getTrackerClient();
				
				if ( an != null ){
					
					TRTrackerAnnouncerResponse last_announce_response = an.getLastResponse();
					
					if ( 	now - last_announce > 15*60*1000 ||
							last_announce_response == null ||
							last_announce_response.getStatus() == TRTrackerAnnouncerResponse.ST_OFFLINE ||
							force ){
	
						dm.setUserData( DM_ANNOUNCE_KEY, new Long( now ));
						
						Logger.log(	new LogEvent(LOGID, "    updating tracker for " + dm.getDisplayName()));
	
						dm.requestTrackerAnnounce( true );
					}
				}
			}
		}
		
	    PluginInterface dht_tracker_pi = getPluginManager().getPluginInterfaceByClass( DHTTrackerPlugin.class );

	    if ( dht_tracker_pi != null ){
	    	
	    	((DHTTrackerPlugin)dht_tracker_pi.getPlugin()).announceAll();
	    }
	}
	
	public LocaleUtil
	getLocaleUtil()
	{
		return( LocaleUtil.getSingleton());
	}
	
	public void
	start()
	
		throws AzureusCoreException
	{
		AEThread2.setOurThread();
		
		try{
			this_mon.enter();
		
			if ( started ){
				
				throw( new AzureusCoreException( "Core: already started" ));
			}
			
			if ( stopped ){
				
				throw( new AzureusCoreException( "Core: already stopped" ));
			}
			
			started	= true;
			
		}finally{
			
			this_mon.exit();
		}
		
		// If a user sets this property, it is an alias for the following settings.
		if ("1".equals(System.getProperty("azureus.safemode"))) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Safe mode enabled"));
			
			Constants.isSafeMode = true;
			System.setProperty("azureus.loadplugins", "0");
			System.setProperty("azureus.disabledownloads", "1");
			System.setProperty("azureus.skipSWTcheck", "1");
			
			// Not using localised text - not sure it's safe to this early.
			Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogEvent.LT_WARNING,
				"You are running " + Constants.APP_NAME + " in safe mode - you " +
					"can change your configuration, but any downloads added will " +
					"not be remembered when you close " + Constants.APP_NAME + "."
			));
		}
		
	   /**
	    * test to see if UI plays nicely with a really slow initialization
	    */
	   String sDelayCore = System.getProperty("delay.core", null);
	   if (sDelayCore != null) {
	  	 try {
	  		 long delayCore = Long.parseLong(sDelayCore);
	  		 Thread.sleep(delayCore);
	  	 } catch (Exception e) {
	  		 e.printStackTrace();
	  	 }
	   }


		// run plugin loading in parallel to the global manager loading
		AEThread2 pluginload = new AEThread2("PluginLoader",true)
		{
			public void run() {
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, "Loading of Plugins starts"));
				pi.loadPlugins(AzureusCoreImpl.this, false, !"0".equals(System.getProperty("azureus.loadplugins")), true, true);
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, "Loading of Plugins complete"));
			}
		};
		
		if (LOAD_PLUGINS_IN_OTHER_THREAD) {
			pluginload.start();
		}
		else {
			pluginload.run();
		}


		


		// Disable async loading of existing torrents, because there are many things
		// (like hosting) that require all the torrents to be loaded.  While we
		// can write code for each of these cases to wait until the torrents are
		// loaded, it's a pretty big job to find them all and fix all their quirks.
		// Too big of a job for this late in the release stage.
		// Other example is the tracker plugin that is coded in a way where it must 
		// always publish a complete rss feed
		
		global_manager = GlobalManagerFactory.create(
				this,
				new GlobalMangerProgressListener()
				{
					public void 
					reportCurrentTask(
						String currentTask )
					{
						initialisation_op.reportCurrentTask( currentTask );
					}
					  
					public void 
					reportPercent(
						int percent )
					{
						initialisation_op.reportPercent( percent );
					}
				}, 0);
		
		if (stopped) {
			System.err.println("Core stopped while starting");
			return;
		}

		// wait until plugin loading is done
		if (LOAD_PLUGINS_IN_OTHER_THREAD) {
			pluginload.join();
		}

		if (stopped) {
			System.err.println("Core stopped while starting");
			return;
		}
		
		VuzeFileHandler.getSingleton().addProcessor(
				new VuzeFileProcessor()
				{
					public void
					process(
						VuzeFile[]		files,
						int				expected_types )
					{
						for (int i=0;i<files.length;i++){
							
							VuzeFile	vf = files[i];
							
							VuzeFileComponent[] comps = vf.getComponents();
							
							for (int j=0;j<comps.length;j++){
								
								VuzeFileComponent comp = comps[j];
								
								int	comp_type = comp.getType();
								
								if ( comp_type == VuzeFileComponent.COMP_TYPE_ADD_TORRENT ){
									
									PluginInterface default_pi = getPluginManager().getDefaultPluginInterface();
									
									Map map = comp.getContent();
									
									try{
										Torrent torrent;

										String url = MapUtils.getMapString(map, "torrent_url", null );
																				
										if ( url != null ){
											
											TorrentDownloader dl = default_pi.getTorrentManager().getURLDownloader( new URL( url ));
											
											torrent = dl.download();
																						
										}else{
											
											String tf = MapUtils.getMapString(map, "torrent_file", null );

											if ( tf != null ){
												
												File file = new File( tf );
												
												if ( !file.canRead() || file.isDirectory()){
													
													throw( new Exception( "torrent_file '" + tf + "' is invalid" ));
												}
												
												torrent = default_pi.getTorrentManager().createFromBEncodedFile(file);
																								
											}else{
												
												throw( new Exception( "torrent_url or torrent_file must be specified" ));
											}
										}
										
										File	dest = null;
										
										String save_folder = MapUtils.getMapString(map, "save_folder", null );
										
										if ( save_folder != null ){
											
											dest = new File( save_folder, torrent.getName());
											
										}else{
											
											String save_file = MapUtils.getMapString(map, "save_file", null );

											if ( save_file != null ){
												
												dest = new File( save_file );
											}
										}

										if ( dest != null ){
											
											dest.getParentFile().mkdirs();
										}
										
										default_pi.getDownloadManager().addDownload( torrent, null, dest );

									}catch( Throwable e ){
										
										Debug.out( e );
									}
									
									comp.setProcessed();
								}
							}
						}
					}
				});
		
		
		
		triggerLifeCycleComponentCreated(global_manager);

		pi.initialisePlugins();

		if (stopped) {
			System.err.println("Core stopped while starting");
			return;
		}
		
		if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "Initializing Plugins complete"));

		try{
			PluginInterface dht_pi 	= getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );

			if ( dht_pi != null ){
				
				dht_pi.addEventListener(
					new PluginEventListener()
					{
						private boolean	first_dht = true;
						
						public void
						handleEvent(
							PluginEvent	ev )
						{
							if ( ev.getType() == DHTPlugin.EVENT_DHT_AVAILABLE ){
								
								if ( first_dht ){
									
									first_dht	= false;
								
									DHT 	dht = (DHT)ev.getValue();
									
									dht.addListener(
										new DHTListener()
										{
											public void 
											speedTesterAvailable(
												DHTSpeedTester tester ) 
											{
												if ( speed_manager != null ){
												
													speed_manager.setSpeedTester( tester );
												}
											}
										});
																
									global_manager.addListener(
											new GlobalManagerAdapter()
											{
												public void 
												seedingStatusChanged( 
													boolean seeding_only_mode,
													boolean	b )
												{
													checkConfig();
												}
											});
									
									COConfigurationManager.addAndFireParameterListeners(
										new String[]{	TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY,
														TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY },
										new ParameterListener()
										{
											public void 
											parameterChanged(
												String parameterName )
											{
												checkConfig();
											}
										});
										
								}
							}
						}
						
						protected void
						checkConfig()
						{
							if ( speed_manager != null ){
							
								speed_manager.setEnabled( TransferSpeedValidator.isAutoSpeedActive(global_manager) );
							}
						}
						
					});
			}
		}catch( Throwable e ){
		}
		
	   if ( COConfigurationManager.getBooleanParameter( "Resume Downloads On Start" )){
	   
		   global_manager.resumeDownloads();
	   }
	    
	   VersionCheckClient.getSingleton().initialise();

	   instance_manager.initialize();

	   NetworkManager.getSingleton().initialize(this); 
         
	   SpeedLimitHandler.getSingleton( this );
	   
	   Runtime.getRuntime().addShutdownHook( new AEThread("Shutdown Hook") {
	     public void runSupport() {
			Logger.log(new LogEvent(LOGID, "Shutdown hook triggered" ));
			AzureusCoreImpl.this.stop();
	     }
	   });	
	   	  

	   DelayedTask delayed_task = 
	   		UtilitiesImpl.addDelayedTask(
	   			"Core", 
	   			new Runnable()
	   			{
	   				public void
	   				run()
	   				{
	   					new AEThread2( "core:delayTask", true )
	   					{
	   						public void
	   						run()
	   						{				
			   					AEDiagnostics.checkDumpsAndNatives();
		
			   					COConfigurationManager.setParameter( "diags.enable.pending.writes", true );
			   					
			   					AEDiagnostics.flushPendingLogs();
			   					
			   					NetworkAdmin na = NetworkAdmin.getSingleton();
		
			   					na.runInitialChecks(AzureusCoreImpl.this);
		
			   					na.addPropertyChangeListener(
			   							new NetworkAdminPropertyChangeListener()
			   							{
			   								private String	last_as;
		
			   								public void
			   								propertyChanged(
			   										String		property )
			   								{
			   									NetworkAdmin na = NetworkAdmin.getSingleton();
		
			   									if ( property.equals( NetworkAdmin.PR_NETWORK_INTERFACES )){
		
			   										boolean	found_usable = false;
		
			   										NetworkAdminNetworkInterface[] intf = na.getInterfaces();
		
			   										for (int i=0;i<intf.length;i++){
		
			   											NetworkAdminNetworkInterfaceAddress[] addresses = intf[i].getAddresses();
		
			   											for (int j=0;j<addresses.length;j++){
		
			   												if ( !addresses[j].isLoopback()){
		
			   													found_usable = true;
			   												}
			   											}
			   										}
		
			   										// ignore event if nothing usable
		
			   										if ( !found_usable ){
		
			   											return;
			   										}
		
			   										Logger.log(	new LogEvent(LOGID, "Network interfaces have changed (new=" + na.getNetworkInterfacesAsString() + ")"));
		
			   										announceAll( false );
		
			   									}else if ( property.equals( NetworkAdmin.PR_AS )){
		
			   										String	as = na.getCurrentASN().getAS();
		
			   										if ( last_as == null ){
		
			   											last_as = as;
		
			   										}else if ( !as.equals( last_as )){
		
			   											Logger.log(	new LogEvent(LOGID, "AS has changed (new=" + as + ")" ));
		
			   											last_as = as;
		
			   											announceAll( false );
			   										}
			   									}
			   								}
			   							});
			   					
			   					setupSleepAndCloseActions();
	   						}
	   					}.start();
	   				}
	   			});

	   delayed_task.queue();

			if (stopped) {
				System.err.println("Core stopped while starting");
				return;
			}

	   PairingManagerFactory.getSingleton();
	   
	   AzureusCoreRunningListener[] runningListeners;
	   mon_coreRunningListeners.enter();
	   try {
	  	 if (coreRunningListeners == null) {
	  		 runningListeners = new AzureusCoreRunningListener[0];
	  	 } else {
	  		 runningListeners = coreRunningListeners.toArray(new AzureusCoreRunningListener[0]);
	  		 coreRunningListeners = null;
	  	 }
	  	 
	   } finally {
	  	 mon_coreRunningListeners.exit();
	   }

		// Trigger Listeners now that core is started
		new AEThread2("Plugin Init Complete", false )
		{
			public void
			run()
			{
				try{
					PlatformManagerFactory.getPlatformManager().startup( AzureusCoreImpl.this );
					
				}catch( Throwable e ){
					
					Debug.out( "PlatformManager: init failed", e );
				}
				
				Iterator	it = lifecycle_listeners.iterator();
				
				while( it.hasNext()){
					
					try{
						AzureusCoreLifecycleListener listener = (AzureusCoreLifecycleListener)it.next();
						
						if ( !listener.requiresPluginInitCompleteBeforeStartedEvent()){
						
							listener.started( AzureusCoreImpl.this );
						}
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
				}
				
				pi.initialisationComplete();
				
				it = lifecycle_listeners.iterator();
				
				while( it.hasNext()){
					
					try{
						AzureusCoreLifecycleListener listener = (AzureusCoreLifecycleListener)it.next();
						
						if ( listener.requiresPluginInitCompleteBeforeStartedEvent()){
						
							listener.started( AzureusCoreImpl.this );
						}				
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
				}
			}
		}.start();

		// Typicially there are many runningListeners, most with quick execution, and 
		// a few longer ones.  Let 3 run at a time, queue the rest.  Without
		// a ThreadPool, the slow ones would delay the startup processes that run
		// after this start() method
		ThreadPool tp = new ThreadPool("Trigger AzureusCoreRunning Listeners", 3);
		for (final AzureusCoreRunningListener l : runningListeners) {
			try {
				tp.run(new AERunnable() {
					public void runSupport() {
						l.azureusCoreRunning(AzureusCoreImpl.this);
					}
				});
			} catch (Throwable t) {
				Debug.out(t);
			}
		}
		
		// Debug.out("Core Start Complete");
	}

	public boolean 
	isInitThread() 
	{
		return( AEThread2.isOurThread( Thread.currentThread()));
	}
	
	public boolean
	isStarted()
	{
	   mon_coreRunningListeners.enter();
	   try {
	  	 return( started && coreRunningListeners == null );
	   } finally {
	  	 mon_coreRunningListeners.exit();
	   }
	}
	
	public void 
	triggerLifeCycleComponentCreated(
		AzureusCoreComponent component )
	{
		Iterator it = lifecycle_listeners.iterator();
		
		while( it.hasNext()){

			try{
				((AzureusCoreLifecycleListener)it.next()).componentCreated(this, component);
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	private void
	runNonDaemon(
		final Runnable	r )
	
		throws AzureusCoreException
	{
		if ( !Thread.currentThread().isDaemon()){
			
			r.run();
			
		}else{
			
			final AESemaphore	sem = new AESemaphore( "AzureusCore:runNonDaemon" );
			
			final Throwable[]	error = {null};
			
			new AEThread2( "AzureusCore:runNonDaemon", false )
			{
				public void
				run()
				{
					try{
			
						r.run();
						
					}catch( Throwable e ){
						
						error[0]	= e;
						
					}finally{
						
						sem.release();
					}
				}
			}.start();
			
			sem.reserve();
			
			if ( error[0] != null ){
	
				if ( error[0] instanceof AzureusCoreException ){
					
					throw((AzureusCoreException)error[0]);
					
				}else{
					
					throw( new AzureusCoreException( "Operation failed", error[0] ));
				}			
			}
		}
	}
	
	public void
	stop()
	
		throws AzureusCoreException
	{
		runNonDaemon(new AERunnable() {
			public void runSupport() {
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, "Stop operation starts"));

				stopSupport(true);
			}
		});
	}
	
	private void
	stopSupport(
		boolean		apply_updates )
	
		throws AzureusCoreException
	{
		AEDiagnostics.flushPendingLogs();
		
		boolean	wait_and_return = false;
		
		try{
			this_mon.enter();
		
			if ( stopped ){
				
					// ensure config is saved as there may be pending changes to persist and we've got here
					// via a shutdown hook
									
				COConfigurationManager.save();
				
				wait_and_return = true;
				
			}else{
			
				stopped	= true;
				
				if ( !started ){
					
					Logger.log(new LogEvent(LOGID, "Core not started"));
					
						// might have been marked dirty due to core being created to allow functions to be used but never started...
					
					if ( AEDiagnostics.isDirty()){
						
						AEDiagnostics.markClean();
					}
					
					stopping_sem.releaseForever();
					
					return;
				}		
			}
		}finally{
			
			this_mon.exit();
		}
		
		if ( wait_and_return ){
			
			Logger.log(new LogEvent(LOGID, "Waiting for stop to complete"));
			
			stopping_sem.reserve();
			
			return;
		}
		
		SimpleTimer.addEvent(
			"ShutFail",
			SystemTime.getOffsetTime( 30*1000 ),
			new TimerEventPerformer()
			{
				boolean	die_die_die;
				
				public void 
				perform(
					TimerEvent event )
				{
					AEDiagnostics.dumpThreads();
					
					if ( die_die_die ){
					
						Debug.out( "Shutdown blocked, force exiting" );
						
						SESecurityManager.exitVM(0);
					}
					
					die_die_die = true;
					
					SimpleTimer.addEvent( "ShutFail", SystemTime.getOffsetTime( 30*1000 ), this );
				}
			});
				
		List	sync_listeners 	= new ArrayList();
		List	async_listeners	= new ArrayList();
		
		Iterator it = lifecycle_listeners.iterator();
			
		while( it.hasNext()){
			AzureusCoreLifecycleListener	l = (AzureusCoreLifecycleListener)it.next();
			
			if ( l.syncInvokeRequired()){
				sync_listeners.add( l );
			}else{
				async_listeners.add( l );
			}
		}
		
		try{
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Invoking synchronous 'stopping' listeners"));

			for (int i=0;i<sync_listeners.size();i++){		
				try{
					((AzureusCoreLifecycleListener)sync_listeners.get(i)).stopping( this );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
			
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Invoking asynchronous 'stopping' listeners"));

				// in case something hangs during listener notification (e.g. version check server is down
				// and the instance manager tries to obtain external address) we limit overall dispatch
				// time to 10 seconds
			
			ListenerManager.dispatchWithTimeout(
					async_listeners,
					new ListenerManagerDispatcher()
					{
						public void
						dispatch(
							Object		listener,
							int			type,
							Object		value )
						{
							((AzureusCoreLifecycleListener)listener).stopping( AzureusCoreImpl.this );
						}
					},
					10*1000 );
	
			
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Stopping global manager"));

			if (global_manager != null) {
				global_manager.stopGlobalManager();
			}
			
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Invoking synchronous 'stopped' listeners"));

			for (int i=0;i<sync_listeners.size();i++){		
				try{
					((AzureusCoreLifecycleListener)sync_listeners.get(i)).stopped( this );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
			
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Invoking asynchronous 'stopped' listeners"));

			ListenerManager.dispatchWithTimeout(
					async_listeners,
					new ListenerManagerDispatcher()
					{
						public void
						dispatch(
							Object		listener,
							int			type,
							Object		value )
						{
							((AzureusCoreLifecycleListener)listener).stopped( AzureusCoreImpl.this );
						}
					},
					10*1000 );
			
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Waiting for quiescence"));

			NonDaemonTaskRunner.waitUntilIdle();
			
				// shut down diags - this marks the shutdown as tidy and saves the config
			
			AEDiagnostics.markClean();
	
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Stop operation completes"));
	
				// if any installers exist then we need to closedown via the updater
			
			if ( 	apply_updates && 
					getPluginManager().getDefaultPluginInterface().getUpdateManager().getInstallers().length > 0 ){
				
				AzureusRestarterFactory.create( this ).restart( true );
			}
			
			try {
				Class c = Class.forName( "sun.awt.AWTAutoShutdown" );
	      
				if (c != null) {
					c.getMethod( "notifyToolkitThreadFree", new Class[]{} ).invoke( null, new Object[]{} );
				}
			} catch (Throwable t) {
			}
			
			if ( ca_shutdown_computer_after_stop ){
				
				if ( apply_updates ){
					
						// best we can do here is wait a while for updates to be applied
					try{
						Thread.sleep( 10*1000 );
						
					}catch( Throwable e ){
						
					}
				}
				
				try{
					PlatformManagerFactory.getPlatformManager().shutdown( PlatformManager.SD_SHUTDOWN );
					
				}catch( Throwable e ){
					
					Debug.out( "PlatformManager: shutdown failed", e );
				}
			}
		
			try{
				ThreadGroup	tg = Thread.currentThread().getThreadGroup();
				
				while( tg.getParent() != null ){
					
					tg = tg.getParent();
				}
				
				Thread[]	threads = new Thread[tg.activeCount()+1024];
				
				tg.enumerate( threads, true );
				
				boolean	bad_found = false;
				
				for (int i=0;i<threads.length;i++){
					
					Thread	t = threads[i];
										
					if ( t != null && t.isAlive() && t != Thread.currentThread() && !t.isDaemon() && !AEThread2.isOurThread( t )){
					
						bad_found = true;
						
						break;
					}
				}
				
				if ( bad_found ){
										
					new AEThread2( "VMKiller", true )
					{
						public void
						run()
						{
							try{
								Thread.sleep(10*1000);
								
								ThreadGroup	tg = Thread.currentThread().getThreadGroup();
								
								Thread[]	threads = new Thread[tg.activeCount()+1024];
								
								tg.enumerate( threads, true );
								
								String	bad_found = "";
								
								for (int i=0;i<threads.length;i++){
									
									Thread	t = threads[i];
																		
									if ( t != null && t.isAlive() && !t.isDaemon() && !AEThread2.isOurThread( t )){
									
										String	details = t.getName();
										
										StackTraceElement[] trace = t.getStackTrace();
										
										if ( trace.length > 0 ){
											
											details += "[";
											
											for ( int j=0;j<trace.length;j++ ){
											
												details += (j==0?"":",") + trace[j];
											}
											
											details += "]";
										}
										
										bad_found += (bad_found.length()==0?"":", ") + details;
									}
								}
								
								Debug.out( "Non-daemon thread(s) found: '" + bad_found + "' - force closing VM" );
								
								SESecurityManager.exitVM(0);
								
							}catch( Throwable e ){
								
							}
						}
					}.start();
	
				}
			}catch( Throwable e ){
			}
			
	
		}finally{
			
			stopping_sem.releaseForever();
		}
	}
	
	
	public void
	requestStop()
	
		throws AzureusCoreException
	{
		if (stopped)
			return;

		runNonDaemon(new AERunnable() {
			public void runSupport() {

				Iterator it = lifecycle_listeners.iterator();
				
				while( it.hasNext()){

					if (!((AzureusCoreLifecycleListener)it.next())
							.stopRequested(AzureusCoreImpl.this)) {
						if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
									"Request to stop the core has been denied"));

						return;
					}
				}

				stop();
			}
		});
	}
	
	public void
	restart()
	
		throws AzureusCoreException
	{
		runNonDaemon(new AERunnable() {
			public void runSupport() {
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, "Restart operation starts"));

				checkRestartSupported();

				restarting = true;
				
				stopSupport(false);

				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, "Restart operation: stop complete,"
							+ "restart initiated"));

				AzureusRestarterFactory.create(AzureusCoreImpl.this).restart(false);
			}
		});
	}
	
	public void
	requestRestart()
	
		throws AzureusCoreException
	{
		runNonDaemon(new AERunnable() {
            public void runSupport() {
                checkRestartSupported();

                Iterator it = lifecycle_listeners.iterator();
                
                while( it.hasNext()){
                    AzureusCoreLifecycleListener l = (AzureusCoreLifecycleListener)it.next();

                    if (!l.restartRequested(AzureusCoreImpl.this)) {

                        if (Logger.isEnabled())
                            Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
                                    "Request to restart the core"
                                            + " has been denied"));

                        return;
                    }
                }

                restart();
            }
        });
	}
	
	public boolean
	isRestarting()
	{
		return( restarting );
	}
	
	public void
	checkRestartSupported()
	
		throws AzureusCoreException
	{
		if ( getPluginManager().getPluginInterfaceByClass( "org.gudy.azureus2.update.UpdaterPatcher") == null ){
			Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
					"Can't restart without the 'azupdater' plugin installed"));
			
			throw( new  AzureusCoreException("Can't restart without the 'azupdater' plugin installed"));
		}
	}
	
	public void
	saveState()
	{
		GlobalManager	gm = global_manager;
		
		if ( gm != null ){
			
			gm.saveState();
		}
		
		COConfigurationManager.save();
	}
	
	public GlobalManager
	getGlobalManager()
	
		throws AzureusCoreException
	{
		if ( global_manager == null ){
			
			throw( new AzureusCoreException( "Core not running" ));
		}
		
		return( global_manager );
	}
	
	public TRHost
	getTrackerHost()
	
		throws AzureusCoreException
	{	
		return( TRHostFactory.getSingleton());
	}
	
	public PluginManagerDefaults
	getPluginManagerDefaults()
	
		throws AzureusCoreException
	{
		return( PluginManager.getDefaults());
	}
	
	public PluginManager
	getPluginManager()
	
		throws AzureusCoreException
	{
			// don't test for running here, the restart process calls this after terminating the core...
		
		return( PluginInitializer.getDefaultInterface().getPluginManager());
	}
	
	public IpFilterManager
	getIpFilterManager()
	
		throws AzureusCoreException
	{
		return( IpFilterManagerFactory.getSingleton());
	}
	
	public AZInstanceManager
	getInstanceManager()
	{
		return( instance_manager );
	}
	
	public SpeedManager
	getSpeedManager()
	{
		return( speed_manager );
	}
	
	public CryptoManager
	getCryptoManager()
	{
		return( crypto_manager );
	}
	
	public NATTraverser
	getNATTraverser()
	{
		return( nat_traverser );
	}
	
	private void
	setupSleepAndCloseActions()
	{
		if ( PlatformManagerFactory.getPlatformManager().hasCapability( PlatformManagerCapabilities.PreventComputerSleep )){
			
			COConfigurationManager.addAndFireParameterListeners(
				new String[]{
					"Prevent Sleep Downloading",
					"Prevent Sleep FP Seeding",
				},
				new ParameterListener()
				{
					private TimerEventPeriodic timer_event;

					public void 
					parameterChanged(
						String parameterName )
					{
						synchronized( this ){
					
							boolean dl = COConfigurationManager.getBooleanParameter( "Prevent Sleep Downloading" );
							boolean se = COConfigurationManager.getBooleanParameter( "Prevent Sleep FP Seeding" );
							
							boolean	active = dl || se;
								
							try{
								setPreventComputerSleep( PlatformManagerFactory.getPlatformManager(), active, "config change" );
								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
							
							
							if ( !active ){
								
								if ( timer_event != null ){
									
									timer_event.cancel();
									
									timer_event = null;
								}
							}else{
							
								if ( timer_event == null ){
									
									timer_event = 
										SimpleTimer.addPeriodicEvent(
												"core:sleepAct",
												2*60*1000,
												new TimerEventPerformer()
												{
													public void 
													perform(
														TimerEvent event )
													{
														if ( !stopped ){
														
															checkSleepActions();
														}
													}
												});
								}
							}
						}
					}
				});
		}
		
		COConfigurationManager.addAndFireParameterListeners(
			new String[]{
					"On Downloading Complete Do",
					"On Seeding Complete Do"
			},
			new ParameterListener()
			{
				private TimerEventPeriodic timer_event;
				
				public void 
				parameterChanged(
					String parameterName )
				{
					String	dl_act = COConfigurationManager.getStringParameter( "On Downloading Complete Do" );
					String	se_act = COConfigurationManager.getStringParameter( "On Seeding Complete Do" );
					
					synchronized( this ){
						
						boolean	dl_nothing 	= dl_act.equals( "Nothing" );
						boolean se_nothing	= se_act.equals( "Nothing" );
						
						if ( dl_nothing ){
							
							ca_last_time_downloading	= -1;
						}
						
						if ( se_nothing ){
							
							ca_last_time_seeding	= -1;
						}
				
						if ( dl_nothing && se_nothing ){
							
							if ( timer_event != null ){
								
								timer_event.cancel();
								
								timer_event = null;
							}
						}else{
						
							if ( timer_event == null ){
								
								timer_event = 
									SimpleTimer.addPeriodicEvent(
											"core:closeAct",
											30*1000,
											new TimerEventPerformer()
											{
												public void 
												perform(
													TimerEvent event )
												{
													if ( !stopped ){
													
														checkCloseActions();
													}
												}
											});
							}
							
							checkCloseActions();
						}
					}
				}
			});
	}

	protected void
	checkSleepActions()
	{
		boolean ps_downloading 	= COConfigurationManager.getBooleanParameter( "Prevent Sleep Downloading" );
		boolean ps_fp_seed	 	= COConfigurationManager.getBooleanParameter( "Prevent Sleep FP Seeding" );

		String	declining_subsystems = "";
		
		for ( PowerManagementListener l: power_listeners ){
			
			try{
				if ( !l.requestPowerStateChange( PowerManagementListener.ST_SLEEP, null )){
					
					declining_subsystems += (declining_subsystems.length()==0?"":",") + l.getPowerName();
				}
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		if ( declining_subsystems.length() == 0 && !( ps_downloading || ps_fp_seed )){

			return;
		}

		PlatformManager platform = PlatformManagerFactory.getPlatformManager();

		boolean	prevent_sleep 	= false;
		String	prevent_reason	= null;

		if ( declining_subsystems.length() > 0 ){
			
			prevent_sleep 	= true;
			prevent_reason 	= "subsystems declined sleep: " +  declining_subsystems;
			
		}else{
			
			List<DownloadManager> managers = getGlobalManager().getDownloadManagers();

			for ( DownloadManager manager: managers ){
	
				int state = manager.getState();
				
				if ( 	state == DownloadManager.STATE_FINISHING ||
						manager.getDownloadState().getFlag( DownloadManagerState.FLAG_METADATA_DOWNLOAD )){
	
					if ( ps_downloading ){
	
						prevent_sleep 	= true;
						prevent_reason	= "active downloads";
						
						break;
					}
	
				}else{
	
					if ( state == DownloadManager.STATE_DOWNLOADING ){
	
						PEPeerManager pm = manager.getPeerManager();
	
						if ( pm != null ){
	
							if ( pm.hasDownloadablePiece()){
	
								if ( ps_downloading ){
	
									prevent_sleep 	= true;
									prevent_reason	= "active downloads";
	
									break;
								}
							}else{
	
									// its effectively seeding, change so logic about recheck obeyed below
	
								state = DownloadManager.STATE_SEEDING;
							}
						}
					}
	
					if ( state == DownloadManager.STATE_SEEDING && ps_fp_seed ){
	
						DiskManager disk_manager = manager.getDiskManager();
	
						if ( disk_manager != null && disk_manager.getCompleteRecheckStatus() != -1 ){
	
								// wait until recheck is complete before we mark as downloading-complete
	
							if ( ps_downloading ){
	
								prevent_sleep 	= true;
								prevent_reason	= "active downloads";
	
								break;
							}
	
						}else{
	
							try{
								DefaultRankCalculator calc = StartStopRulesDefaultPlugin.getRankCalculator( PluginCoreUtils.wrap( manager ));
	
								if ( calc.getCachedIsFP()){
									
									prevent_sleep 	= true;
									prevent_reason	= "first-priority seeding";
	
									break;
								}
							}catch( Throwable e ){
	
							}
						}
					}
				}
			}
		}
		
		if ( prevent_sleep != platform.getPreventComputerSleep()){
			
			if ( prevent_sleep ){
				
				prevent_sleep_remove_trigger = false;
				
			}else{
				
				if ( !prevent_sleep_remove_trigger ){
					
					prevent_sleep_remove_trigger = true;
					
					return;
				}
			}
			
			if ( prevent_reason == null ){
				
				if ( ps_downloading && ps_fp_seed ){
					
					prevent_reason = "no active downloads or first-priority seeding";
					
				}else if ( ps_downloading ){
					
					prevent_reason = "no active downloads";
					
				}else{
					
					prevent_reason = "no active first-priority seeding";
				}
			}

			setPreventComputerSleep( platform, prevent_sleep, prevent_reason );
		}
	}
	
	private void
	setPreventComputerSleep(
		PlatformManager		platform,
		boolean				prevent_sleep,
		String				prevent_reason )
	{
		for ( PowerManagementListener l: power_listeners ){
			
			try{
				l.informPowerStateChange( PowerManagementListener.ST_SLEEP, new Object[]{ prevent_sleep, prevent_reason });
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		Logger.log( new LogEvent(LOGID, "Computer sleep prevented state changed to '" + prevent_sleep + "' due to " + prevent_reason ));	

		try{
			platform.setPreventComputerSleep( prevent_sleep );
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	protected void
	checkCloseActions()
	{
		List<DownloadManager> managers = getGlobalManager().getDownloadManagers();
		
		boolean	is_downloading 	= false;
		boolean is_seeding		= false;
		
		for ( DownloadManager manager: managers ){
			
			if ( manager.isPaused()){
			
					// if anything's paused we don't want to trigger any actions as something transient (e.g. speed test) is going on
				
				return;
			}
			
			if ( manager.getDownloadState().getFlag( DownloadManagerState.FLAG_METADATA_DOWNLOAD )){
				
				return;
			}
			
			int state = manager.getState();
			
			if ( state == DownloadManager.STATE_FINISHING ){

				is_downloading = true;
				
			}else{
			
				if ( state == DownloadManager.STATE_DOWNLOADING ){
					
					PEPeerManager pm = manager.getPeerManager();
					
					if ( pm != null ){
						
						if ( pm.hasDownloadablePiece()){
							
							is_downloading = true;
							
						}else{
				
								// its effectively seeding, change so logic about recheck obeyed below
							
							state = DownloadManager.STATE_SEEDING;
						}
					}
				}
				
				if ( state == DownloadManager.STATE_SEEDING ){
				
					DiskManager disk_manager = manager.getDiskManager();
	
					if ( disk_manager != null && disk_manager.getCompleteRecheckStatus() != -1 ){
					
							// wait until recheck is complete before we mark as downloading-complete
						
						is_downloading	= true;
						
					}else{
						
						is_seeding		= true;
					}
				}
			}
		}
		
		long	now = SystemTime.getMonotonousTime();
		
		if ( is_downloading ){
			
			ca_last_time_downloading 	= now;
			ca_last_time_seeding		= -1;
			
		}else if ( is_seeding ){
			
			ca_last_time_seeding = now;
		}
		
		String	dl_act = COConfigurationManager.getStringParameter( "On Downloading Complete Do" );
		
		if ( !dl_act.equals( "Nothing" )){
		
			if ( ca_last_time_downloading >= 0 && !is_downloading && now - ca_last_time_downloading >= 30*1000 ){
				
				executeCloseAction( true, true, dl_act, null );
			}
		}
		
		String	se_act = COConfigurationManager.getStringParameter( "On Seeding Complete Do" );

		if ( !se_act.equals( "Nothing" )){
			
			if ( ca_last_time_seeding >= 0 && !is_seeding && now - ca_last_time_seeding >= 30*1000 ){
				
				executeCloseAction( true, false, se_act, null );
			}
		}
	}
	
	public void
	executeCloseAction(
		String		action,
		String		reason )
	{
		executeCloseAction( false, false, action, reason );
	}
	
	private void
	executeCloseAction(
		final boolean	obey_reset,
		final boolean	download_trigger,
		final String	action,
		final String	reason )
	{
			// prevent retriggering on resume from standby
		
		ca_last_time_downloading	= -1;
		ca_last_time_seeding		= -1;
		
		boolean reset = obey_reset && COConfigurationManager.getBooleanParameter( "Stop Triggers Auto Reset" );
		
		if ( reset ){
			
			if ( download_trigger ){
			
				COConfigurationManager.setParameter( "On Downloading Complete Do", "Nothing" );
				
			}else{
				
				COConfigurationManager.setParameter( "On Seeding Complete Do", "Nothing" );
			}
		}
		
		String type_str		= reason==null?MessageText.getString( "core.shutdown." + (download_trigger?"dl":"se")):reason;
		String action_str 	= MessageText.getString( "ConfigView.label.stop." + action );
				
		String message = 
			MessageText.getString( 
				"core.shutdown.alert",
				new String[]{
					action_str,
					type_str,
				});
		
		UIFunctions ui_functions = UIFunctionsManager.getUIFunctions();
		
		if ( ui_functions != null ){
		
			ui_functions.forceNotify( UIFunctions.STATUSICON_NONE, null, message, null, new Object[0], -1 );
		}
		
		Logger.log( 
			new LogAlert( 
				LogAlert.UNREPEATABLE, 
				LogEvent.LT_INFORMATION,
				message ));

		new DelayedEvent(
			"CoreShutdown",
			10*1000,
			new AERunnable()
			{
				public void
				runSupport()
				{
					Logger.log( new LogEvent(LOGID, "Executing close action '" + action + "' due to " + (download_trigger?"downloading":"seeding") + " completion" ));					
					
						// quit vuze -> quit
						// shutdown computer -> quit vuze + shutdown
						// sleep/hibernate = announceAll and then sleep/hibernate with Vuze still running
					
					if ( action.equals( CA_QUIT_VUZE )){
						
						requestStop();
					
					}else if ( action.equals( CA_SLEEP ) || action.equals( CA_HIBERNATE )){

						announceAll( true );
						
						try{
							PlatformManagerFactory.getPlatformManager().shutdown( 
									action.equals( CA_SLEEP )?PlatformManager.SD_SLEEP:PlatformManager.SD_HIBERNATE );
							
						}catch( Throwable e ){
							
							Debug.out( "PlatformManager: shutdown failed", e );
						}
						
					}else if ( action.equals( CA_SHUTDOWN )){

						ca_shutdown_computer_after_stop = true;
						
						requestStop();
						
					}else if ( action.startsWith( "RunScript" )){
						
						String script;
						
						if ( download_trigger ){
							
							script = COConfigurationManager.getStringParameter( "On Downloading Complete Script", "" );
							
						}else{
							
							script = COConfigurationManager.getStringParameter( "On Seeding Complete Script", "" );
						}
						
						File script_file = new File( script.trim());
						
						if ( !script_file.isFile()){
							
							Logger.log( new LogEvent(LOGID, "Script failed to run - '" + script_file + "' isn't a valid script file" ));
							
							Debug.out( "Invalid script: " + script_file );
							
						}else{
							
							try{
								boolean	close_vuze = action.equals( "RunScriptAndClose" );
								
								if ( !close_vuze ){
									
										// assume script might implement a sleep
									
									announceAll( true );
								}
								
								getPluginManager().getDefaultPluginInterface().getUtilities().createProcess( script_file.getAbsolutePath());
								
								if ( close_vuze ){
									
									requestStop();
								}
								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
						
					}else{
						
						Debug.out( "Unknown close action '" + action + "'" );
					}
				}
			});
	}
	
	public AzureusCoreOperation
	createOperation(
		final int		type )
	{
		AzureusCoreOperation	op =
			new AzureusCoreOperation()
			{
				public int
				getOperationType()
				{
					return( type );
				}
				
				public AzureusCoreOperationTask 
				getTask() 
				{
					return null;
				}
				
				public void 
				reportCurrentTask(
					String task )
				{
					AzureusCoreImpl.this.reportCurrentTask( this, task );
				}
				  
				public void 
				reportPercent(
					int percent )
				{
					AzureusCoreImpl.this.reportPercent( this, percent );
				}
			};
			
		for (int i=0;i<operation_listeners.size();i++){
			
			try{
				((AzureusCoreOperationListener)operation_listeners.get(i)).operationCreated( op );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( op );
	}
	
	public void
	createOperation(
		final int					type,
		AzureusCoreOperationTask	task )
	{
		final AzureusCoreOperationTask[] f_task = { task };
		
		AzureusCoreOperation	op =
				new AzureusCoreOperation()
				{
					public int
					getOperationType()
					{
						return( type );
					}
					
					public AzureusCoreOperationTask 
					getTask() 
					{
						return( f_task[0] );
					}
					
					public void 
					reportCurrentTask(
						String task )
					{
						AzureusCoreImpl.this.reportCurrentTask( this, task );
					}
					  
					public void 
					reportPercent(
						int percent )
					{
						AzureusCoreImpl.this.reportPercent( this, percent );
					}
				};
				

		for (int i=0;i<operation_listeners.size();i++){
			
				// don't catch exceptions here as we want errors from task execution to propagate
				// back to the invoker
			
			if (((AzureusCoreOperationListener)operation_listeners.get(i)).operationCreated( op )){
				
				f_task[0] = null;
			}
		}
		
			// nobody volunteeered to run it for us, we'd better do it
		
		if ( f_task[0] != null ){
			
			task.run( op );
		}
	}
	
	protected void 
	reportCurrentTask(
		AzureusCoreOperation		op,			
		String 						currentTask )
	{
		if ( op.getOperationType() == AzureusCoreOperation.OP_INITIALISATION ){
			
			PluginInitializer.fireEvent( PluginEvent.PEV_INITIALISATION_PROGRESS_TASK, currentTask );
		}
		
		Iterator it = listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((AzureusCoreListener)it.next()).reportCurrentTask( op, currentTask );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	  
	protected void 
	reportPercent(
		AzureusCoreOperation		op,	
		int 						percent )
	{
		if ( op.getOperationType() == AzureusCoreOperation.OP_INITIALISATION ){

			PluginInitializer.fireEvent( PluginEvent.PEV_INITIALISATION_PROGRESS_PERCENT, new Integer( percent ));
		}

		Iterator it = listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((AzureusCoreListener)it.next()).reportPercent( op, percent );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void
	addLifecycleListener(
		AzureusCoreLifecycleListener	l )
	{
		lifecycle_listeners.add(l);
	}
	
	public void
	removeLifecycleListener(
		AzureusCoreLifecycleListener	l )
	{
		lifecycle_listeners.remove(l);
	}
	
	public void
	addListener(
		AzureusCoreListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		AzureusCoreListener	l )
	{
		listeners.remove( l );
	}
	
	public void
	addOperationListener(
		AzureusCoreOperationListener	l )
	{
		operation_listeners.add(l);
	}
	
	public void
	removeOperationListener(
		AzureusCoreOperationListener	l )
	{
		operation_listeners.remove(l);
	}

	public static void addCoreRunningListener(AzureusCoreRunningListener l) {
	   mon_coreRunningListeners.enter();
	   try {
    		if (AzureusCoreImpl.coreRunningListeners != null) {
    			coreRunningListeners.add(l);
    			
    			return;
    		}
	   } finally {
	  	 mon_coreRunningListeners.exit();
	   }
	   
	   l.azureusCoreRunning(AzureusCoreImpl.getSingleton());
	}
	
	public void
	addPowerManagementListener(
		PowerManagementListener	listener )
	{
		power_listeners.add( listener );
	}
		
	public void
	removePowerManagementListener(
		PowerManagementListener	listener )
	{
		power_listeners.remove( listener );
	}	
}
