package pk.qwerty12.nfclockscreenoffenabler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class NFCLockScreenOffEnablerActivity extends PreferenceActivity {

	private CheckBoxPreference mEnableTagLostCheckBox = null;
	private ListPreference mEnableNfcForStatesList = null;
	private Preference mCopyrightPreference = null;
	private EditTextPreference mPresenceCheckTimeoutPreference = null;
	private MultiSelectListPreference mSoundsToPlay = null;
	private SharedPreferences mPrefs;

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return super.getSharedPreferences(Common.PREFS, Context.MODE_WORLD_READABLE);
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("WorldReadableFiles")
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.pref_general);

		getViews();

		mPrefs = getSharedPreferences(Common.PREFS, Context.MODE_WORLD_READABLE);

		migrateOldSettings(mPrefs);

		/* Workaround: since we now do the changes automatically as this activity
		 * follows our XML now (overriden getSharedPreferences), emitting an intent
		 * to update settings won't work since the changes are committed after the return.
		 * We can workaround that in two ways: either delay the intent using a handler,
		 * but that may cause some multiple intents to be fired for no reason, or 
		 * saving the changes manually then firing the intent. We do the latter here.
		 */
		OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preferenceChanged(preference, newValue);
				return true;
			}
		};

		mEnableTagLostCheckBox.setChecked(mPrefs.getBoolean(Common.PREF_TAGLOST, true));
		mEnableTagLostCheckBox.setOnPreferenceChangeListener(listener);

		mSoundsToPlay.setValues(mPrefs.getStringSet(Common.PREF_SOUNDS_TO_PLAY, 
				new HashSet<String>(Arrays.asList(
						getResources().getStringArray(R.array.pref_default_sounds_to_play)))));
		mSoundsToPlay.setOnPreferenceChangeListener(listener);

		mEnableNfcForStatesList.setSummary("   "); // required or will not update
		mEnableNfcForStatesList.setSummary("%s");
		mEnableNfcForStatesList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {	
				preferenceChanged(preference, newValue);
				mEnableNfcForStatesList.setSummary("   "); // required or will not update
				mEnableNfcForStatesList.setSummary("%s");
				return true;
			}
		});

		mCopyrightPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Resources resources = getResources();
				String[] contributors = resources.getStringArray(R.array.contributors);
				String[] methodsOfContribution = resources.getStringArray(R.array.contributors_how);
				String[] translators = resources.getStringArray(R.array.translators);
				String translatorsTitle = getString(R.string.translators_title);

				String contributorString = "";

				for (int i = 0; i < contributors.length; i++) {
					if (i != 0)
						contributorString += "\n";
					String contributor = "* %s (%s)";
					contributorString += String.format(contributor, contributors[i], methodsOfContribution[i]);
				}
				
				if (translators.length != 0) {
					contributorString += "\n\n";
					contributorString += translatorsTitle + ":" + "\n";
					
					for (int i = 0; i < translators.length; i++) {
						if (i != 0)
							contributorString += "\n";
						contributorString += "* " + translators[i];
					}
				}

				AlertDialog.Builder alertDialog = new AlertDialog.Builder(NFCLockScreenOffEnablerActivity.this)
				.setTitle(R.string.contributors_title)
				.setMessage(contributorString);

				alertDialog.show();
				return true;
			}
		});

		if (mPrefs.contains(Common.PREF_PRESENCE_CHECK_TIMEOUT)) {
			mPresenceCheckTimeoutPreference.setSummary(getString(R.string.pref_summary_presence_check)
					+ " " + mPrefs.getString(Common.PREF_PRESENCE_CHECK_TIMEOUT, "2000"));
		}
		mPresenceCheckTimeoutPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preferenceChanged(preference, newValue);
				mPresenceCheckTimeoutPreference.setSummary(" ");
				mPresenceCheckTimeoutPreference.setSummary(getString(R.string.pref_summary_presence_check)
						+ " " + newValue);
				return true;
			}
		});

		findPreference(Common.PREF_DEBUG_MODE).setOnPreferenceChangeListener(listener);
		Preference authorizedNfcPreference = findPreference("nfc_tags");
		authorizedNfcPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(NFCLockScreenOffEnablerActivity.this, NfcTags.class));
				return false;
			}
		});

		findPreference("show_in_launcher").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {	
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Thanks to Chainfire for this
				// http://www.chainfire.eu/articles/133/_TUT_Supporting_multiple_icons_in_your_app/
				PackageManager pm = getPackageManager();
				pm.setComponentEnabledSetting(
						new ComponentName(getApplicationContext(), Common.MOD_PACKAGE_NAME + ".Activity-Launcher"), 
						(Boolean) newValue ? 
								PackageManager.COMPONENT_ENABLED_STATE_ENABLED : 
									PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
									PackageManager.DONT_KILL_APP
						);
				return true;
			}
		});
	}

	/* The old settings style was very broken in my opinion, rather than using proper
	 * values, it relied on a boolean, and that was very confusing to anyone reading
	 * the code without knowing anything beforehand.
	 * Also a requested feature was to turn off NFC sounds, and having two options for
	 * sounds isn't very user friendly, so we changed that to a multi-select list where
	 * the user can select what sounds they want.
	 */
	@SuppressWarnings("deprecation")
	private void migrateOldSettings(SharedPreferences prefs) {
		boolean didWeMigrate = false;
		Editor editor = prefs.edit();
		if (prefs.contains(Common.PREF_PLAY_TAG_LOST_SOUND)) {
			HashSet<String> defaultSounds = 
					new HashSet<String>(Arrays.asList(
							getResources().getStringArray(R.array.pref_default_sounds_to_play)));
			boolean wasTagLostSoundEnabled = 
					prefs.getBoolean(Common.PREF_PLAY_TAG_LOST_SOUND, true);
			if (wasTagLostSoundEnabled) {
				defaultSounds.add("sound_taglost");
				editor.putStringSet(Common.PREF_SOUNDS_TO_PLAY, defaultSounds);
			}

			editor.remove(Common.PREF_PLAY_TAG_LOST_SOUND);
			didWeMigrate = true;
		}

		if (prefs.contains(Common.PREF_PRESENCE_CHECK_TIMEOUT_OLD)) {
			int oldValue = prefs.getInt(Common.PREF_PRESENCE_CHECK_TIMEOUT_OLD, 2000);
			editor.putString(Common.PREF_PRESENCE_CHECK_TIMEOUT, String.valueOf(oldValue));
			editor.remove(Common.PREF_PRESENCE_CHECK_TIMEOUT_OLD);
			didWeMigrate = true;
		}

		if (prefs.contains(Common.PREF_LOCKED)) {
			boolean oldValue = prefs.getBoolean(Common.PREF_LOCKED, true);
			String newValue = oldValue ? "locked_screen_on" : "screen_off";
			editor.putString(Common.PREF_ENABLE_NFC_WHEN, newValue);
			editor.remove(Common.PREF_LOCKED);
			didWeMigrate = true;
		}

		editor.commit();

		if (didWeMigrate) {
			emitSettingsChanged();
			Toast.makeText(this, R.string.settings_migrated, Toast.LENGTH_SHORT).show();
		}
	}

	@SuppressWarnings("deprecation")
	private void getViews() {
		mEnableTagLostCheckBox = (CheckBoxPreference) findPreference(Common.PREF_TAGLOST);
		mEnableNfcForStatesList = (ListPreference) findPreference("enable_nfc_for_lock_state");
		mCopyrightPreference = (Preference) findPreference("copyright_key");
		mPresenceCheckTimeoutPreference = (EditTextPreference) findPreference(Common.PREF_PRESENCE_CHECK_TIMEOUT);
		mSoundsToPlay = (MultiSelectListPreference) findPreference(Common.PREF_SOUNDS_TO_PLAY);
	}

	protected void emitSettingsChanged() {
		Intent i = new Intent(Common.SETTINGS_UPDATED_INTENT);
		sendBroadcast(i);
	}

	@SuppressWarnings("unchecked")
	private void preferenceChanged(Preference preference, Object newValue) {
		Editor editor = mPrefs.edit();
		if (newValue instanceof String) {
			editor.putString(preference.getKey(), (String) newValue);
		} else if (newValue instanceof Integer) {
			editor.putInt(preference.getKey(), (Integer) newValue);
		} else if (newValue instanceof Set<?>) {
			editor.putStringSet(preference.getKey(), (Set<String>) newValue);
		} else if (newValue instanceof Boolean) {
			editor.putBoolean(preference.getKey(), (Boolean) newValue);
		}

		editor.commit();
		emitSettingsChanged();
	}
}
