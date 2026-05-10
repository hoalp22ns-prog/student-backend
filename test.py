import pandas as pd

# File input
INPUT_FILE = r"D:\VS\DTDM\URL dataset.csv"

# File output
OUTPUT_FILE = "dataset_converted.csv"

# Đọc CSV
df = pd.read_csv(INPUT_FILE)

# Đổi tên cột
df = df.rename(columns={"type": "label"})

# Đổi giá trị label
df["label"] = df["label"].replace({
    "legitimate": "safe"
})

# Lưu file mới
df.to_csv(OUTPUT_FILE, index=False)

print("✅ Converted successfully!")
print(df.head())