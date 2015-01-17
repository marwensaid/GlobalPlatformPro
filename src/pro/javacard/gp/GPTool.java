package pro.javacard.gp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CardTerminals.State;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.TerminalFactory;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import pro.javacard.gp.GPData.KeyType;
import pro.javacard.gp.GPKeySet.Diversification;
import pro.javacard.gp.GPKeySet.GPKey;
import pro.javacard.gp.GPKeySet.GPKey.Type;
import apdu4j.APDUReplayProvider;
import apdu4j.HexUtils;
import apdu4j.LoggingCardTerminal;
import apdu4j.TerminalManager;


public class GPTool {

	private final static String CMD_INFO = "info";

	private final static String CMD_LIST = "list";
	private final static String CMD_LOCK = "lock";
	private final static String CMD_INSTALL = "install";
	private final static String CMD_UNINSTALL = "uninstall";
	private final static String CMD_DELETE = "delete";
	private final static String CMD_CREATE = "create";
	private final static String CMD_LOAD = "load";
	private final static String CMD_UNLOCK = "unlock";
	private final static String CMD_MAKE_DEFAULT = "make-default";
	private final static String CMD_APDU = "apdu";
	private final static String CMD_SECURE_APDU = "secure-apdu";
	private final static String OPT_SCP = "scp";

	private final static String OPT_DELETEDEPS = "deletedeps";
	private final static String OPT_DEFAULT = "default";
	private final static String OPT_CAP = "cap";
	private final static String OPT_APPLET = "applet";
	private final static String OPT_PACKAGE = "package";
	private final static String OPT_INSTANCE = "instance";
	private final static String OPT_DO_ALL_READERS = "all";
	private final static String OPT_NOFIX = "nofix";
	private final static String OPT_PARAMS = "params";

	private final static String OPT_CONTINUE = "skip-error";
	private final static String OPT_RELAX = "relax";
	private final static String OPT_READER = "reader";
	private final static String OPT_VERSION = "version";
	private final static String OPT_SDAID = "sdaid";
	private final static String OPT_DEBUG = "debug";
	private final static String OPT_DUMP = "dump";
	private final static String OPT_REPLAY = "replay";
	private final static String OPT_VERBOSE = "verbose";
	private final static String OPT_REINSTALL = "reinstall";
	private final static String OPT_VIRGIN = "virgin";
	private final static String OPT_MODE = "mode";

	private final static String OPT_MAC = "mac";
	private final static String OPT_ENC = "enc";
	private final static String OPT_KEK = "kek";
	private final static String OPT_KEY = "key";
	private final static String OPT_KEY_VERSION = "keyver";
	private final static String OPT_KEY_ID = "keyid";
	private final static String OPT_NEW_KEY_VERSION = "new-keyver";

	private final static String OPT_EMV = "emv";
	private final static String OPT_VISA2 = "visa2";


	private static OptionSet parseArguments(String[] argv) throws IOException {
		OptionSet args = null;
		OptionParser parser = new OptionParser();

		// Generic options
		parser.acceptsAll(Arrays.asList("h", "help"), "Shows this help string").forHelp();
		parser.acceptsAll(Arrays.asList("d", OPT_DEBUG), "Show PC/SC and APDU trace");
		parser.acceptsAll(Arrays.asList("v", OPT_VERBOSE), "Be verbose about operations");
		parser.acceptsAll(Arrays.asList("r", OPT_READER), "Use specific reader").withRequiredArg();
		parser.acceptsAll(Arrays.asList("l", CMD_LIST), "List the contents of the card");
		parser.acceptsAll(Arrays.asList("i", CMD_INFO), "Show information");
		parser.acceptsAll(Arrays.asList("a", CMD_APDU), "Send raw APDU (hex)").withRequiredArg();
		parser.acceptsAll(Arrays.asList("s", CMD_SECURE_APDU), "Send raw APDU (hex) via SCP").withRequiredArg();
		parser.accepts(OPT_DUMP, "Dump APDU communication to <File>").withRequiredArg().ofType(File.class);
		parser.accepts(OPT_REPLAY, "Replay APDU responses from <File>").withRequiredArg().ofType(File.class);

		parser.accepts(OPT_VERSION, "Show information about the program");

		// Special options
		parser.accepts(OPT_RELAX, "Relaxed error checking");
		parser.accepts(OPT_DO_ALL_READERS, "Work with multiple readers");
		parser.accepts(OPT_NOFIX, "Do not try to fix PCSC/Java/OS issues");


		// Applet operation options
		parser.accepts(OPT_CAP, "Use a CAP file as source").withRequiredArg().ofType(File.class);
		parser.accepts(CMD_LOAD, "Load a CAP file").withRequiredArg().ofType(File.class);

		parser.accepts(CMD_INSTALL, "Install applet").withOptionalArg().ofType(File.class);
		parser.accepts(OPT_PARAMS, "Installation parameters").withRequiredArg();

		parser.accepts(CMD_UNINSTALL, "Uninstall applet/package").withRequiredArg().ofType(File.class);
		parser.accepts(OPT_DEFAULT, "Indicate Default Selected");
		parser.accepts(OPT_DELETEDEPS, "Also delete dependencies");
		parser.accepts(OPT_REINSTALL, "Remove card content during installation");
		parser.accepts(CMD_MAKE_DEFAULT, "Make AID the default").withRequiredArg().withValuesConvertedBy(ArgMatchers.aid());

		parser.accepts(CMD_DELETE, "Delete something").requiredIf(OPT_DELETEDEPS).withOptionalArg().withValuesConvertedBy(ArgMatchers.aid());

		parser.accepts(CMD_CREATE, "Create new instance of an applet").withRequiredArg().withValuesConvertedBy(ArgMatchers.aid());
		parser.accepts(OPT_APPLET, "Applet AID").withRequiredArg().withValuesConvertedBy(ArgMatchers.aid());
		parser.accepts(OPT_PACKAGE, "Package AID").withRequiredArg().withValuesConvertedBy(ArgMatchers.aid());
		parser.accepts(OPT_INSTANCE, "Instance AID").withRequiredArg().withValuesConvertedBy(ArgMatchers.aid());

		// Key options
		parser.accepts(OPT_MAC, "Specify MAC key").withRequiredArg().withValuesConvertedBy(ArgMatchers.key());
		parser.accepts(OPT_ENC, "Specify ENC key").withRequiredArg().withValuesConvertedBy(ArgMatchers.key());
		parser.accepts(OPT_KEK, "Specify KEK key").withRequiredArg().withValuesConvertedBy(ArgMatchers.key());
		parser.accepts(OPT_KEY, "Specify master key").withRequiredArg().withValuesConvertedBy(ArgMatchers.key());
		parser.accepts(OPT_KEY_ID, "Specify key ID").withRequiredArg().ofType(Integer.class);
		parser.accepts(OPT_KEY_VERSION, "Specify key version").withRequiredArg().ofType(Integer.class);
		parser.accepts(CMD_LOCK, "Set new key").withRequiredArg().withValuesConvertedBy(ArgMatchers.key());
		parser.accepts(CMD_UNLOCK, "Set default key");
		parser.accepts(OPT_SCP, "Force the use of SCP0X").withRequiredArg().ofType(Integer.class);
		parser.accepts(OPT_NEW_KEY_VERSION, "key version for the new key").withRequiredArg().ofType(Integer.class);

		parser.accepts(OPT_VIRGIN, "Card has virgin keys");


		// Key diversification and AID options
		parser.accepts(OPT_EMV, "Use EMV diversification");
		parser.accepts(OPT_VISA2, "Use VISA2 diversification");
		parser.accepts(OPT_MODE, "APDU mode to use (mac/enc/clr)").withRequiredArg().withValuesConvertedBy(ArgMatchers.mode());;

		parser.accepts(OPT_SDAID, "ISD AID").withRequiredArg().withValuesConvertedBy(ArgMatchers.aid());


		// Parse arguments
		try {
			args = parser.parse(argv);
			// Try to fetch all values so that format is checked before usage
			for (String s: parser.recognizedOptions().keySet()) {args.valuesOf(s);}
		} catch (OptionException e) {
			if (e.getCause() != null) {
				System.err.println(e.getMessage() + ": " + e.getCause().getMessage());
			} else {
				System.err.println(e.getMessage());
			}
			System.err.println();
			parser.printHelpOn(System.err);
			System.exit(1);
		}

		// Do the work, based on arguments
		if (args.has("help")) {
			parser.printHelpOn(System.out);
			System.exit(0);
		}

		return args;
	}

	public static void main(String[] argv) throws Exception {

		OptionSet args = parseArguments(argv);

		if (args.has(OPT_VERSION)) {
			System.out.println("OpenKMS GlobalPlatform version " + GlobalPlatform.sdk_version);
		}

		// Parameters for opening the secure channel
		// Assume a single master key
		GPKeySet ks = null;
		if (args.has(OPT_KEY)) {
			ks = new GPKeySet((GPKeySet.GPKey)args.valueOf(OPT_KEY));
		} else {
			ks = new GPKeySet(new GPKey(GPData.defaultKey, Type.DES3));
		}
		// override if needed
		if (args.has(OPT_MAC)) {
			ks.setKey(KeyType.MAC, (GPKeySet.GPKey)args.valueOf(OPT_MAC));
		}
		if (args.has(OPT_ENC)) {
			ks.setKey(KeyType.ENC, (GPKeySet.GPKey)args.valueOf(OPT_ENC));
		}
		if (args.has(OPT_KEK)) {
			ks.setKey(KeyType.KEK, (GPKeySet.GPKey)args.valueOf(OPT_KEK));
		}

		// Key ID and Version
		if (args.has(OPT_KEY_ID)) {
			ks.setKeyID((int) args.valueOf(OPT_KEY_ID));
		}
		if (args.has(OPT_KEY_VERSION)) {
			ks.setKeyVersion((int) args.valueOf(OPT_KEY_VERSION));
		}

		// Set diversification if specified
		if (args.has(OPT_VISA2)) {
			ks.diversification = Diversification.VISA2;
		} else if (args.has(OPT_EMV)) {
			ks.diversification = Diversification.EMV;
		}

		// Load a CAP file, if specified
		CapFile cap = null;
		if (args.has(OPT_CAP)) {
			File capfile = (File) args.valueOf(OPT_CAP);
			cap = new CapFile(new FileInputStream(capfile));
			if (args.has(OPT_VERBOSE)) {
				System.out.println("**** CAP info:");
				cap.dump(System.out);
			}
		}

		// Now actually talk to possible terminals
		try {
			TerminalFactory tf = TerminalManager.getTerminalFactory(args.has(OPT_NOFIX) ? false : true);

			// Replay responses from a file
			if (args.has(OPT_REPLAY)) {
				File f = (File) args.valueOf(OPT_REPLAY);
				tf = TerminalFactory.getInstance("PC/SC", new FileInputStream(f), new APDUReplayProvider());
			}

			CardTerminals terminals = tf.terminals();

			// List terminals if needed
			if (args.has(OPT_DEBUG)) {
				System.out.println("# Detected readers from " + tf.getProvider().getName());
				for (CardTerminal term : terminals.list()) {
					System.out.println((term.isCardPresent() ? "[*] " : "[ ] ") + term.getName());
				}
			}

			// Select terminals to work on
			List<CardTerminal> do_readers;
			if (args.has(OPT_READER)) {
				String reader = (String) args.valueOf(OPT_READER);
				CardTerminal t = terminals.getTerminal(reader);
				if (t == null) {
					System.err.println("Reader \"" + reader + "\" not found.");
					System.exit(1);
				}
				do_readers = Arrays.asList(t);
			} else {
				do_readers = terminals.list(State.CARD_PRESENT);
				if (do_readers.size() > 1 && !args.hasArgument(OPT_DO_ALL_READERS)) {
					System.err.println("More than one reader with a card found.");
					System.err.println("Run with --"+OPT_DO_ALL_READERS+" to work with all found cards");
					System.exit(1);
				}
			}

			// Work all readers
			for (CardTerminal reader: do_readers) {
				// Wrap with logging if requested
				if (args.has(OPT_DEBUG)) {
					// And with APDU dumping
					OutputStream o = null;
					if (args.has(OPT_DUMP)) {
						File f = (File) args.valueOf(OPT_DUMP);
						o = new FileOutputStream(f);
					}
					reader = LoggingCardTerminal.getInstance(reader, o);
				}

				Card card = null;
				try {
					// Establish connection
					try {
						card = reader.connect("*");
						card.beginExclusive();
					} catch (CardException e) {
						if (args.has(OPT_CONTINUE)) {
							e.printStackTrace();
							continue;
						} else {
							throw e;
						}
					}

					// GlobalPlatform specific
					GlobalPlatform gp = new GlobalPlatform(card.getBasicChannel());
					if (args.has(OPT_VERBOSE))
						gp.beVerboseTo(System.out);

					// Disable strict mode if requested
					gp.setStrict(!args.has(OPT_RELAX));

					if (args.has(CMD_INFO) || args.has(OPT_VERBOSE)) {
						System.out.println("Reader: " + reader.getName());
						System.out.println("ATR: " + HexUtils.encodeHexString(card.getATR().getBytes()));
						System.out.println("More information about your card:");
						System.out.println("    http://smartcard-atr.appspot.com/parse?ATR="+HexUtils.encodeHexString(card.getATR().getBytes()));
						System.out.println();
					}

					// Send all raw APDU-s to the default-selected application of the card
					if (args.has(CMD_APDU)) {
						for (Object s: args.valuesOf(CMD_APDU)) {
							CommandAPDU c = new CommandAPDU(HexUtils.stringToBin((String)s));
							card.getBasicChannel().transmit(c);
						}
					}

					// Talk to the card manager (can be null)
					gp.select((AID) args.valueOf(OPT_SDAID));

					// Fetch some possibly interesting data
					if (args.has(CMD_INFO)) {
						System.out.println("***** Card info:");
						GPData.print_card_info(gp);
					}

					// check for possible diversification for virgin cards
					if (Arrays.equals(ks.getKey(KeyType.MAC).getValue(), GPData.defaultKey) && args.has(OPT_VIRGIN) && !args.has(OPT_RELAX)) {
						if (GPData.suggestDiversification(gp.getCPLC()) != Diversification.NONE && ks.getKeyVersion() == 0x00) {
							System.err.println("A virgin card that has not been used with GlobalPlatformPro before");
							System.err.println("probably requires EMV diversification but is not asked for.");
							System.err.println("Use -emv for EMV diversification. Or don't run with -virgin or use -relax.");
							System.exit(1);
						}
					}

					// Authenticate, only if needed
					if (args.has(CMD_LIST) || args.has(CMD_INSTALL) || args.has(CMD_DELETE)
							|| args.has(CMD_CREATE) || args.has(CMD_LOCK) || args.has(CMD_UNLOCK)
							|| args.has(CMD_MAKE_DEFAULT) || args.has(CMD_UNINSTALL) || args.has(CMD_SECURE_APDU)) {

						// Override default mode if needed.
						if (args.has(OPT_MODE)) {
							gp.defaultMode.clear();
							gp.defaultMode.add((GlobalPlatform.APDUMode) args.valueOf(OPT_MODE));
						}

						// Override SCP version
						int scp_version = 0;
						if (args.has(OPT_SCP)) {
							scp_version = (int) args.valueOf(OPT_SCP);
						}

						// Possibly brick the card now, if keys don't match.
						gp.openSecureChannel(ks, null, scp_version, gp.defaultMode);

						// --secure-apdu or -s
						if (args.has(CMD_SECURE_APDU)) {
							for (Object s: args.valuesOf(CMD_SECURE_APDU)) {
								CommandAPDU c = new CommandAPDU(HexUtils.stringToBin((String)s));
								gp.transmit(c);
							}
						}

						// --delete <aid> or --delete --default
						if (args.has(CMD_DELETE)) {
							if (args.has(OPT_DEFAULT)) {
								gp.uninstallDefaultSelected(args.has(OPT_DELETEDEPS));
							}
							@SuppressWarnings("unchecked")
							List<AID> aids = (List<AID>) args.valuesOf(CMD_DELETE);
							for (AID aid: aids) {
								try {
									gp.deleteAID(aid, args.has(OPT_DELETEDEPS));
								} catch (GPException e) {
									if (!gp.getRegistry().allAIDs().contains(aid)) {
										System.out.println("Could not delete AID (not present on card): " + aid);
									} else {
										System.out.println("Could not delete AID: " + aid);
										if (e.sw == 0x6985) {
											System.out.println("TIP: Maybe try with --" + OPT_DELETEDEPS);
										}
										throw e;
									}
								}
							}
						}

						// --uninstall <cap>
						if (args.has(CMD_UNINSTALL)) {
							File capfile = (File) args.valueOf(CMD_UNINSTALL);
							CapFile instcap = new CapFile(new FileInputStream(capfile));
							AID aid = instcap.getPackageAID();
							if (!gp.getRegistry().allAIDs().contains(aid)) {
								System.out.println(aid + " is not present on card!");
							} else {
								gp.deleteAID(aid, true);
								System.out.println(aid + " deleted.");
							}
						}

						// --install <applet.cap>
						if (args.has(CMD_INSTALL)) {
							AID def = gp.getRegistry().getDefaultSelectedPackageAID();
							if (def != null && args.has(OPT_DEFAULT)) {
								if (args.has(OPT_REINSTALL)) {
									gp.verbose("Removing current default applet/package");
									// Remove all instances of default selected app package
									gp.deleteAID(def, true);
								}
							}

							File capfile = (File) args.valueOf(CMD_INSTALL);
							CapFile instcap = new CapFile(new FileInputStream(capfile));

							if (args.has(OPT_VERBOSE)) {
								instcap.dump(System.out);
							}
							// Take the applet AID from CAP but allow to override
							AID aid = instcap.getAppletAIDs().get(0);
							if (args.has(OPT_APPLET))
								aid = (AID) args.valueOf(OPT_APPLET);

							if (gp.getRegistry().allAIDs().contains(aid)) {
								System.err.println("WARNING: Applet " + aid + " already present on card");
							}

							gp.verbose("Installing applet from package " + instcap.getPackageName());
							try {
								gp.loadCapFile(instcap);
							} catch (GPException e) {
								if (e.sw == 0x6985) {
									System.err.println("Applet loading failed. Are you sure the CAP file version is compatible with your card?");
								} else {
									throw e;
								}
							}
							byte[] params = null;
							if (args.has(OPT_PARAMS)) {
								params = HexUtils.stringToBin((String) args.valueOf(OPT_PARAMS));
							}
							gp.installAndMakeSelectable(instcap.getPackageAID(), aid, null, args.has(OPT_DEFAULT) ? (byte) 0x04 : 0x00, params, null);
						}

						// --create <aid> (--applet <aid> --package <aid> or --cap <cap>)
						if (args.has(CMD_CREATE)) {
							AID packageAID = null;
							AID appletAID = null;
							// Load from cap if present
							if (cap != null) {
								packageAID = cap.getPackageAID();
								appletAID = cap.getAppletAIDs().get(0);
							}
							// override if needed
							packageAID = (AID) args.valueOf(OPT_PACKAGE);
							appletAID = (AID) args.valueOf(OPT_APPLET);

							// check
							if (packageAID == null || appletAID == null)
								throw new IllegalArgumentException("Need --" + OPT_PACKAGE + " and --" + OPT_APPLET + " or --" + OPT_CAP);

							// shoot
							AID instanceAID = (AID) args.valueOf(CMD_CREATE);
							gp.installAndMakeSelectable(packageAID, appletAID, instanceAID, (byte) 0x00, null, null);
						}

						// --list
						if (args.has(CMD_LIST)) {
							for (AIDRegistryEntry e : gp.getRegistry()) {
								AID aid = e.getAID();
								System.out.println("AID: " + HexUtils.encodeHexString(aid.getBytes()) + " (" + GPUtils.byteArrayToReadableString(aid.getBytes()) + ")");
								System.out.println("     " + e.getKind().toShortString() + " " + e.getLifeCycleString() + ": " + e.getPrivilegesString());

								for (AID a : e.getExecutableAIDs()) {
									System.out.println("     " + HexUtils.encodeHexString(a.getBytes()) + " (" + GPUtils.byteArrayToReadableString(a.getBytes()) + ")");
								}
								System.out.println();
							}
						}

						// --lock
						if (args.has(CMD_LOCK)) {
							if (args.has(OPT_KEY) || args.has(OPT_MAC) || args.has(OPT_ENC) || args.has(OPT_KEK) && !args.has(OPT_RELAX))
								gp.printStrictWarning("Using --" + CMD_LOCK + " but specifying other keys");
							GPKey new_key = ((GPKey)args.valueOf(CMD_LOCK));
							// Check that
							int new_version = 1;

							if (args.has(OPT_NEW_KEY_VERSION)) {
								new_version = (int) args.valueOf(OPT_NEW_KEY_VERSION);
							}
							List<GPKeySet.GPKey> keys = new ArrayList<GPKeySet.GPKey>();
							keys.add(new GPKeySet.GPKey(new_version, 01, new_key));
							keys.add(new GPKeySet.GPKey(new_version, 02, new_key));
							keys.add(new GPKeySet.GPKey(new_version, 03, new_key));
							// Add new keys if virgin
							if (args.has(OPT_EMV) || args.has(OPT_VISA2) || args.has(OPT_VIRGIN)) {
								gp.putKeys(keys, false);
							} else {
								// normally replace
								gp.putKeys(keys, true);
							}
							System.out.println("Card locked with: " + new_key.toStringKey());
							System.out.println("Write this down, DO NOT FORGET/LOSE IT!");
						}

						// --unlock
						if (args.has(CMD_UNLOCK)) {
							// Write default keys
							List<GPKeySet.GPKey> keys = new ArrayList<GPKeySet.GPKey>();

							// Fetch the current key information to get the used ID-s.
							List<GPKey> current = gp.getKeyInfoTemplate();
							if (current.size() != 3) {
								throw new GPException("Template has bad length!");
							}
							// FIXME: new key must adhere to currently used SCP version.
							GPKey new_key = new GPKey(GPData.defaultKey, gp.getSCPVersion() == 3 ? Type.AES : Type.DES3);

							// FIXME: this looks ugly
							keys.add(new GPKeySet.GPKey(01, current.get(0).getID(), new_key));
							keys.add(new GPKeySet.GPKey(01, current.get(1).getID(), new_key));
							keys.add(new GPKeySet.GPKey(01, current.get(2).getID(), new_key));

							// "add keys" if default factory keys or otherwise virgin card
							// because version FF can not be addressed
							if (args.has(OPT_VIRGIN)) {
								gp.putKeys(keys, false);
							} else {
								// normally replace existing keys
								gp.putKeys(keys, true);
							}
							System.out.println("Default " + new_key.toStringKey() + " set as master key.");
						}

						// --make-default <aid>
						if (args.has(CMD_MAKE_DEFAULT)) {
							gp.makeDefaultSelected((AID) args.valueOf(CMD_MAKE_DEFAULT), (byte) 0x04);
						}
					}
				} catch (GPException e) {
					// All unhandled GP exceptions halt the program unless it is run with -relax
					if (!args.has(OPT_RELAX)) {
						e.printStackTrace();
						System.exit(1);
					}
					e.printStackTrace();
				} catch (CardException e) {
					// Card exceptions skip to the next reader, if available and allowed
					if (args.has(OPT_CONTINUE)) {
						continue;
					} else {
						e.printStackTrace();
						throw e; // No catch.
					}
				} finally {
					if (card != null) {
						card.endExclusive();
						TerminalManager.disconnect(card, true);
					}
				}
			}
		} catch (Exception e) {
			// Sensible wrapper for the different PC/SC exceptions
			if (TerminalManager.getExceptionMessage(e) != null) {
				System.out.println("PC/SC failure: " + TerminalManager.getExceptionMessage(e));
			} else {
				throw e;
			}
		}
		System.exit(0);
	}
}