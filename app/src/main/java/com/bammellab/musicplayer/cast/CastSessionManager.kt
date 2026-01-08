package com.bammellab.musicplayer.cast

import android.content.Context
import android.util.Log
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class representing Cast UI state
 */
data class CastUiState(
    val isCastAvailable: Boolean = false,
    val isCasting: Boolean = false,
    val castDeviceName: String? = null,
    val isConnecting: Boolean = false
)

/**
 * Manages Cast session lifecycle and exposes state via StateFlow.
 */
class CastSessionManager(context: Context) {

    companion object {
        private const val TAG = "CastSessionManager"
    }

    private val _castState = MutableStateFlow(CastUiState())
    val castState: StateFlow<CastUiState> = _castState.asStateFlow()

    private var castContext: CastContext? = null
    private var sessionManager: SessionManager? = null
    private var currentSession: CastSession? = null

    private val castStateListener = CastStateListener { state ->
        Log.d(TAG, "Cast state changed: $state")
        _castState.value = _castState.value.copy(
            isCastAvailable = state != CastState.NO_DEVICES_AVAILABLE
        )
    }

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            Log.d(TAG, "Session starting")
            _castState.value = _castState.value.copy(isConnecting = true)
        }

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            Log.d(TAG, "Session started: $sessionId")
            currentSession = session
            _castState.value = _castState.value.copy(
                isCasting = true,
                isConnecting = false,
                castDeviceName = session.castDevice?.friendlyName
            )
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            Log.e(TAG, "Session start failed: $error")
            currentSession = null
            _castState.value = _castState.value.copy(
                isCasting = false,
                isConnecting = false,
                castDeviceName = null
            )
        }

        override fun onSessionEnding(session: CastSession) {
            Log.d(TAG, "Session ending")
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            Log.d(TAG, "Session ended: $error")
            currentSession = null
            _castState.value = _castState.value.copy(
                isCasting = false,
                isConnecting = false,
                castDeviceName = null
            )
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {
            Log.d(TAG, "Session resuming: $sessionId")
            _castState.value = _castState.value.copy(isConnecting = true)
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            Log.d(TAG, "Session resumed, wasSuspended: $wasSuspended")
            currentSession = session
            _castState.value = _castState.value.copy(
                isCasting = true,
                isConnecting = false,
                castDeviceName = session.castDevice?.friendlyName
            )
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            Log.e(TAG, "Session resume failed: $error")
            currentSession = null
            _castState.value = _castState.value.copy(
                isCasting = false,
                isConnecting = false,
                castDeviceName = null
            )
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            Log.d(TAG, "Session suspended: $reason")
        }
    }

    init {
        try {
            castContext = CastContext.getSharedInstance(context)
            sessionManager = castContext?.sessionManager

            // Add listeners
            castContext?.addCastStateListener(castStateListener)
            sessionManager?.addSessionManagerListener(sessionManagerListener, CastSession::class.java)

            // Check for existing session
            sessionManager?.currentCastSession?.let { session ->
                currentSession = session
                _castState.value = _castState.value.copy(
                    isCasting = true,
                    castDeviceName = session.castDevice?.friendlyName
                )
            }

            // Check initial cast availability
            castContext?.castState?.let { state ->
                _castState.value = _castState.value.copy(
                    isCastAvailable = state != CastState.NO_DEVICES_AVAILABLE
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Cast", e)
        }
    }

    /**
     * Get the current Cast session, if connected.
     */
    fun getCurrentSession(): CastSession? = currentSession

    /**
     * Get the CastContext for UI components (e.g., MediaRouteButton).
     */
    fun getCastContext(): CastContext? = castContext

    /**
     * End the current cast session.
     */
    fun endSession() {
        sessionManager?.endCurrentSession(true)
    }

    /**
     * Clean up listeners when no longer needed.
     */
    fun release() {
        castContext?.removeCastStateListener(castStateListener)
        sessionManager?.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
    }
}
