package com.kriegersoftware.demos.usbinterface.fragment;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.kriegersoftware.demos.usbinterface.R;

public class TerminalFragment extends Fragment{

	private TextView txt_terminal;
	private Button btn_send;
	private EditText et_input;
	private ScrollView scroll_terminal;
	
	private final static int ORIGIN_APP    = 0;
	private final static int ORIGIN_ERROR  = 1;
	private final static int ORIGIN_DEVICE = 2;
	private final static int ORIGIN_USER   = 3;
	private final static int ORIGIN_HELP   = 4;
	
	private final static String[] ORIGIN_TITLE_PREFIX = {
		"System",
		"ERROR",
		"Device ",
		"Input",
		"Help"
	};
	
	private final static int[] ORIGIN_COLOR = {
		Color.parseColor("#0099CC"),
		Color.parseColor("#CC0000"),
		Color.parseColor("#669900"),
		Color.parseColor("#9933CC"),
		Color.parseColor("#FF8800")
	};
	
	private final static String PROMPT_INDICATOR = ":~$";
	private final static String WELCOME_MSG = "Welcome! The USB interface is now online.";
	private final static String HINT_MSG = "Type 'help' for useful commands.";
	private final static String NO_DEVICES  = "No devices are connected.";
	private final static String EMPTY_MSG   = "Message is empty!";
	private final static String HELP_MSG    = "You can use 'clear' to remove all"
											+" content from the screen.\n"
											+"Cleared content will not be added "
											+"to saved history.";
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_terminal, container);
		et_input = (EditText) view.findViewById(R.id.frag_terminal_edittext_input);
		btn_send = (Button) view.findViewById(R.id.frag_terminal_button_send);
		scroll_terminal = (ScrollView) view.findViewById(R.id.frag_terminal_scroll_terminal);
		txt_terminal = (TextView) view.findViewById(R.id.frag_terminal_txt_terminal);
		txt_terminal.setMovementMethod(new ScrollingMovementMethod());
		
		btn_send.setOnClickListener(new SendButtonListener());
		et_input.setOnEditorActionListener(new EditTextEnterListener());
		
		addTextToTerminal(formatTerminalText(null, ORIGIN_APP, WELCOME_MSG));
		addTextToTerminal(formatTerminalText(null, ORIGIN_APP, HINT_MSG));
		addTextToTerminal(formatTerminalText(null, ORIGIN_APP, NO_DEVICES));
		return view;
	}
	
	private void addTextToTerminal(SpannedString input){
		txt_terminal.append(input);
		txt_terminal.append("\n");
		scroll_terminal.post(new Scroller(scroll_terminal,txt_terminal));
		//scroll_terminal.fullScroll(View.FOCUS_DOWN);
	}
	
	private SpannedString formatTerminalText(String origin, int originId,
			String text){
		String result;
		result = ORIGIN_TITLE_PREFIX[originId];
		if(originId==ORIGIN_DEVICE)
			result += origin;
		result += PROMPT_INDICATOR;
		
		SpannableString span1 = new SpannableString(result);
		span1.setSpan(new ForegroundColorSpan(ORIGIN_COLOR[originId]),
				0, result.length(), 0);
		span1.setSpan(new StyleSpan(Typeface.BOLD), 0, result.length(), 0);
		
		SpannableString span2 = new SpannableString(text);
		span2.setSpan(new ForegroundColorSpan(Color.WHITE), 0, text.length(), 0);
		
		return (SpannedString) TextUtils.concat(span1," ",span2);
	}
	
	private void sendInputToDevice(String input){
		if(input.equals("help"))
			addTextToTerminal(formatTerminalText(null, ORIGIN_HELP, HELP_MSG));
		else if(input.equals("clear"))
			txt_terminal.setText("");
		else
			addTextToTerminal(formatTerminalText("PIC18F4550", ORIGIN_DEVICE, "Echo: "+input));
	}
	
	private class EditTextEnterListener implements OnEditorActionListener{
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_SEND) {
	            btn_send.performClick();
	            return true;
	        }
			return false;
		}
	}
	
	private class SendButtonListener implements OnClickListener{
		@Override
		public void onClick(View v) {
			String input=et_input.getText().toString();
			et_input.setText("");
			if(input.equals(""))
				addTextToTerminal(formatTerminalText(null, ORIGIN_ERROR, EMPTY_MSG));
			else{
				addTextToTerminal(formatTerminalText(null, ORIGIN_USER, input));
				sendInputToDevice(input);
			}
		}
	}
	
	private class Scroller implements Runnable {

		ScrollView scroller;
		View       child;

		public Scroller(ScrollView scroller, View child) {
		    this.scroller=scroller; 
		    this.child=child;

		}
		public void run() {
		    scroller.scrollTo(0, child.getBottom());
		}
	}
}
