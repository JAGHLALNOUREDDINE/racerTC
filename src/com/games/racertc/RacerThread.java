package com.games.racertc;

import com.games.racertc.gamestate.GameState;
import com.games.racertc.gamestate.GameStateChangeListener;
import com.games.racertc.gamestate.StateMachine;
import com.games.racertc.ui.UIManager;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.SurfaceHolder;

/**
 * Klasa obslugujaca glowny watek gry. Przechowuje klasy Simulation i Presentation odpowiadajace odpowiednio
 * za symulacje i rysowanie w grze, a takze posredniczy w komunikacji z nimi.
 * @author Piotr Balut
 */
public class RacerThread extends Thread implements GameStateChangeListener {
	
	public final static boolean MEASURE_FPS = true;
	
	/**
	 * Docelowy czas trwania pojedynczej klatki w milisekundach. Obecne
	 * ustawienie to 33 ms, co daje okolo 30 fps.
	 */
	public final static long TARGET_FRAME_DURATION = 33;
	
	final SurfaceHolder surfaceHolder;
	
	final Context context;
	
	final Resources resources;
	
	public RacerThread( SurfaceHolder surfaceHolder, Context context, Resources resources ) {
		this.surfaceHolder = surfaceHolder;
		this.context = context;
		this.resources = resources;
		
		//rejestruje sie jako obserwator zmian stanu gry:
		StateMachine state_machine = StateMachine.getInstance();
		state_machine.addListener( this );
	}

/*-----------------------------------------*/
/*-           Glowna petla gry:           -*/
/*-----------------------------------------*/	
	
	private boolean run = true;
	
	/**
	 * Uruchamia i obsluguje glowna petle gry.
	 */
	@Override
	public void run() {
		
		long previous_time = SystemClock.uptimeMillis();
		//do mierzenia sredniej ilosci fpsow:
		int accumulated_time = 0, frame_count = 0;
		
		while( run ) if( gameState != null ) {
			
			long current_time = SystemClock.uptimeMillis();
			long time_quantum = current_time - previous_time;
			previous_time = current_time;
			
			//Obliczanie FPS'ow:
			if( MEASURE_FPS ) {
				accumulated_time += time_quantum;
				frame_count++;
				if( accumulated_time >= 500 ) {
					UIManager uiman = gameState.getUIManager();
					if( uiman != null )
						uiman.setFPS( (long) ((frame_count / (accumulated_time * 0.001f))) );
					frame_count	= accumulated_time = 0;
				}
			}

			/* Przetwarza stan: */
			if( time_quantum > 0 )
				gameState.process( time_quantum ); //dt - kwant czasu
			
			/* odpal rysowanie: */
			
			//blokujemy canvas
			Canvas canvas = surfaceHolder.lockCanvas();
			gameState.draw( canvas );
			//wysylamy zmiany na ekran:
			surfaceHolder.unlockCanvasAndPost( canvas );
							
			/* Ograniczenie ilosci klatek na sekunde - o ile stan na to pozwala. */
			if( gameState.limitFramerate() ) {
				//oblicza czas jaki watek bedzie spal
				long finished_time = SystemClock.uptimeMillis();
				long sleep_time = TARGET_FRAME_DURATION - ( finished_time - current_time );
				//jezeli zostal jeszcze czas wolny, watek odpoczywa
				if( sleep_time > 0 )
					SystemClock.sleep( sleep_time );
			}
			
		}
		
	}
	
/*-----------------------------------------*/
/*-Implementacja GameStateChangeListener: -*/
/*-----------------------------------------*/			
				
	private GameState gameState = null;
				
	@Override
	public void onGameStateChange( GameState gameState ) {
		this.gameState = gameState;
		//TODO: reakcja na zmiany stanu.
	}
	
}
