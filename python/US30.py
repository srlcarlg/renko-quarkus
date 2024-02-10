from matplotlib import animation
from websocket import create_connection
import json
import pandas as pd
import numpy as np
import mplfinance as mpf

mode_int = 0
while mode_int not in [1, 2, 3]:
    mode_int = input("Select the renko mode (nº): \n "
                 "1 = normal \n 2 = wicks \n 3 = nongap \n")
    try:
        mode_int = int(mode_int)
    except:
        mode_int = mode_int

mode = "normal" if mode_int == 0 else "wicks" if mode_int == 1 else "nongap"

ws = create_connection(f"ws://127.0.0.1:9000/renko/US30/{mode}")
print("Receiving...")

initial_df = {
    "datetime": [1705201200000], "open": [0], "high": [0], "low": [0], "close": [0], "volume": [0]
}
initial_df = pd.DataFrame(initial_df)
initial_df.index = pd.DatetimeIndex(
    pd.to_datetime(initial_df["datetime"].values.astype(np.int64), unit="ms"))

fig, axes = mpf.plot(initial_df, returnfig=True, volume=True,
                     figsize=(11, 8), panel_ratios=(2, 1),
                     title='\nUS30', type='candle', style='charles')
ax1 = axes[0]
ax2 = axes[2]

mpf.plot(initial_df, type='candle', ax=ax1, volume=ax2, axtitle=f'renko: {mode}')

chart_dict = {
    "datetime": [], "open": [], "high": [], "low": [], "close": [], "volume": []
}
def animate(ival):
    result = ws.recv()
    ohlcv = json.loads(result)
    for name in chart_dict.keys():
        chart_dict[name].append(ohlcv[name])

    df = pd.DataFrame(chart_dict)
    df.index = pd.DatetimeIndex(pd.to_datetime(df["datetime"]))

    ax1.clear()
    ax2.clear()

    mpf.plot(df, type='candle', ax=ax1, volume=ax2, axtitle=f'renko: {mode}')

ani = animation.FuncAnimation(fig, animate, interval=50)
mpf.show()

