import os
import pandas as pd

# File paths
input_file = "ppg.csv"
# output_dir = r"P:\GitRepos\School\Researcher\Grant_2026_fall_term_project\data\output"
output_dir = r"P:\GitRepos\School\Researcher\GalaxyWatch-PPG-Logger\server"
output_file = os.path.join(output_dir, "raw_watch_s1.csv")

# 1. Read UTF-16 encoded smartwatch file
df_raw = pd.read_csv(input_file, encoding='utf-16')

# 2. Strip initialization artifacts (e.g., row 0 sensor warm-up sample)
if df_raw.iloc[0]['green'] < 0:
    df_raw = df_raw.iloc[1:].reset_index(drop=True)

# 3. Create standardized dataframe matching the public dataset format
df_formatted = pd.DataFrame({
    'timestamp_ms': df_raw['timestamp'],
    'pleth_1': df_raw['green'],  # Green LED PPG channel
    'pleth_2': df_raw['red'],    # Red LED PPG channel
    'pleth_3': df_raw['ir'],     # Infrared LED PPG channel
})

# 4. Save to standard UTF-8 CSV
os.makedirs(output_dir, exist_ok=True)
df_formatted.to_csv(output_file, index=False, encoding='utf-8')

print(f"Successfully converted {len(df_formatted)} samples to {output_file}")
print("Column format:")
print(df_formatted.head())