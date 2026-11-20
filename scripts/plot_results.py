from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.ticker import ScalarFormatter

RESULTS_DIR = Path("results")
CHARTS_DIR = RESULTS_DIR / "charts"

plt.rcParams.update({
    "figure.dpi": 150,
    "savefig.dpi": 300,
    "font.size": 10,
    "axes.grid": True,
    "grid.alpha": 0.3,
    "axes.spines.top": False,
    "axes.spines.right": False,
})


def set_log2_cutoff_axis(ax, cutoffs):
    ax.set_xscale("log", base=2)
    ticks = [c for c in [1, 2, 4, 8, 16, 32, 64, 128] if c <= max(cutoffs)]
    ax.set_xticks(ticks)
    ax.xaxis.set_major_formatter(ScalarFormatter())


def plot_exp1_random(df: pd.DataFrame):
    sub = df[df["distribution"] == "random"].sort_values("cutoff")
    n_val = int(sub["n"].iloc[0])
    best = sub.loc[sub["median_ms"].idxmin()]
    pure_ms = float(sub.loc[sub["cutoff"] == 1, "median_ms"].iloc[0])

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(13, 5))

    # Trai: Toan bo dai CUTOFF (truc X log2)
    ax1.errorbar(
        sub["cutoff"], sub["median_ms"], yerr=sub["ci95_ms"],
        fmt="-o", capsize=3, linewidth=1.8, markersize=5, color="#1f77b4",
        label="Median ± 95% CI"
    )
    ax1.axhline(pure_ms, color="gray", linestyle=":", linewidth=1.2, label=f"Pure (C=1): {pure_ms:.1f} ms")
    ax1.axvline(7, color="#ff7f0e", linestyle="--", linewidth=1.2, label="Giáo trình cũ (C=7)")
    ax1.axvline(10, color="#2ca02c", linestyle="--", linewidth=1.2, label="Giáo trình mới (C=10)")
    ax1.scatter([best["cutoff"]], [best["median_ms"]], color="#d62728", s=90, zorder=5,
                label=f"Tối ưu (C={int(best['cutoff'])}): {best['median_ms']:.1f} ms")
    set_log2_cutoff_axis(ax1, sub["cutoff"])
    ax1.set_title(f"Toàn dải CUTOFF (1–128) | Random, N={n_val:,}")
    ax1.set_xlabel("CUTOFF (thang log2)")
    ax1.set_ylabel("Thời gian chạy trung vị (ms)")
    ax1.legend(fontsize=8.5)

    # Phai: Phong to vung CUTOFF <= 32 (truc X tuyen tinh) de nhin ro 7 vs 10 vs toi uu
    zoom = sub[sub["cutoff"] <= 32]
    ax2.errorbar(
        zoom["cutoff"], zoom["median_ms"], yerr=zoom["ci95_ms"],
        fmt="-o", capsize=3, linewidth=1.8, markersize=5, color="#1f77b4"
    )
    ax2.axvline(7, color="#ff7f0e", linestyle="--", linewidth=1.2, label="C=7")
    ax2.axvline(10, color="#2ca02c", linestyle="--", linewidth=1.2, label="C=10")
    if best["cutoff"] <= 32:
        ax2.scatter([best["cutoff"]], [best["median_ms"]], color="#d62728", s=90, zorder=5,
                    label=f"Best (C={int(best['cutoff'])})")
    ax2.set_xticks(zoom["cutoff"].tolist())
    ax2.set_title("Phóng to vùng đáy chữ U (CUTOFF 1–32)")
    ax2.set_xlabel("CUTOFF (thang tuyến tính)")
    ax2.set_ylabel("Thời gian chạy trung vị (ms)")
    ax2.legend(fontsize=8.5)

    fig.tight_layout()
    fig.savefig(CHARTS_DIR / "fig1_random_cutoff_curve.png")
    plt.close(fig)


def plot_exp1_distributions(df: pd.DataFrame):
    fig, ax = plt.subplots(figsize=(9.5, 5.5))
    labels = {
        "random": "Random",
        "partially_sorted": "Partially Sorted",
        "reverse": "Reverse Sorted",
        "sorted": "Already Sorted",
        "few_unique": "Few Unique (50 keys)",
    }

    for dist, group in df.groupby("distribution", sort=False):
        g = group.sort_values("cutoff")
        best = g.loc[g["median_ms"].idxmin()]
        ax.plot(
            g["cutoff"], g["speedup_vs_pure_pct"],
            marker="o", markersize=4, linewidth=1.8,
            label=f"{labels.get(dist, dist)} (best C={int(best['cutoff'])}, +{best['speedup_vs_pure_pct']:.1f}%)"
        )

    ax.axhline(0, color="black", linestyle=":", linewidth=1.1, label="Baseline Pure Mergesort (0%)")
    ax.axvline(7, color="#ff7f0e", linestyle="--", alpha=0.8, linewidth=1.1)
    ax.axvline(10, color="#2ca02c", linestyle="--", alpha=0.8, linewidth=1.1)
    set_log2_cutoff_axis(ax, df["cutoff"])

    n_val = int(df["n"].iloc[0])
    ax.set_title(f"Mức cải thiện hiệu năng (%) so với Mergesort thuần theo phân bố (N={n_val:,})")
    ax.set_xlabel("CUTOFF (thang log2)")
    ax.set_ylabel("Cải thiện so với CUTOFF=1 (%)  [cao hơn = nhanh hơn]")
    ax.legend(fontsize=8.5, loc="lower left")

    fig.tight_layout()
    fig.savefig(CHARTS_DIR / "fig2_distributions_speedup.png")
    plt.close(fig)


def plot_exp2_datatypes(df: pd.DataFrame):
    types = df["datatype"].unique().tolist()
    fig, axes = plt.subplots(1, len(types), figsize=(12, 4.8))
    if len(types) == 1:
        axes = [axes]

    for ax, dtype in zip(axes, types):
        sub = df[df["datatype"] == dtype].sort_values("cutoff")
        n_val = int(sub["n"].iloc[0])
        best = sub.loc[sub["median_ms"].idxmin()]

        ax.errorbar(
            sub["cutoff"], sub["median_ms"], yerr=sub["ci95_ms"],
            fmt="-o", capsize=3, linewidth=1.8, markersize=5,
            color="#1f77b4" if dtype == "Integer" else "#9467bd"
        )
        ax.axvline(7, color="#ff7f0e", linestyle="--", linewidth=1.2, label="C=7")
        ax.axvline(10, color="#2ca02c", linestyle="--", linewidth=1.2, label="C=10")
        ax.scatter(
            [best["cutoff"]], [best["median_ms"]], color="#d62728", s=90, zorder=5,
            label=f"Best C={int(best['cutoff'])} (+{best['speedup_vs_pure_pct']:.1f}%)"
        )
        set_log2_cutoff_axis(ax, sub["cutoff"])
        ax.set_title(f"Kiểu {dtype} (Random, N={n_val:,})")
        ax.set_xlabel("CUTOFF (thang log2)")
        ax.set_ylabel("Thời gian chạy trung vị (ms)")
        ax.legend(fontsize=8.5)

    fig.suptitle("Ảnh hưởng của chi phí hàm compareTo(): Integer vs String", fontsize=12, y=1.02)
    fig.tight_layout()
    fig.savefig(CHARTS_DIR / "fig3_datatype_comparison.png", bbox_inches="tight")
    plt.close(fig)


def plot_exp3_doubling(df: pd.DataFrame):
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(13, 5))

    # Trai: Do thi Log-Log kiem chung bac tang truong theo bai 1.3
    for cutoff, group in df.groupby("cutoff", sort=False):
        g = group.sort_values("n")
        slope, _ = np.polyfit(np.log2(g["n"]), np.log2(g["median_ms"]), 1)
        ax1.plot(
            g["n"], g["median_ms"], marker="o", linewidth=1.8,
            label=f"CUTOFF={int(cutoff)} (độ dốc b={slope:.2f})"
        )

    ax1.set_xscale("log", base=2)
    ax1.set_yscale("log", base=2)
    ax1.set_title("Doubling Test (Đồ thị Log-Log)")
    ax1.set_xlabel("Kích thước mảng N (thang log2)")
    ax1.set_ylabel("Thời gian chạy trung vị T(N) [ms] (thang log2)")
    ax1.legend(fontsize=8.5)

    # Phai: Muc cai thien (%) qua cac kich thuoc N
    non_pure = df[df["cutoff"] != 1]
    for cutoff, group in non_pure.groupby("cutoff", sort=False):
        g = group.sort_values("n")
        ax2.plot(
            g["n"] / 1e6, g["speedup_vs_pure_pct"], marker="s", linewidth=1.8,
            label=f"CUTOFF={int(cutoff)}"
        )

    ax2.axhline(0, color="gray", linestyle=":", linewidth=1.1)
    ax2.set_title("Mức cải thiện (%) so với Pure Mergesort theo N")
    ax2.set_xlabel("Kích thước mảng N (triệu phần tử)")
    ax2.set_ylabel("Cải thiện so với CUTOFF=1 (%)")
    ax2.legend(fontsize=8.5)

    fig.tight_layout()
    fig.savefig(CHARTS_DIR / "fig4_doubling_test.png")
    plt.close(fig)


def print_console_summary(df1: pd.DataFrame, df2: pd.DataFrame, df3: pd.DataFrame):
    print("\n=== TOM TAT KET QUA EXP 1 (THEO PHAN BO) ===")
    for dist, g in df1.groupby("distribution", sort=False):
        pure = g.loc[g["cutoff"] == 1, "median_ms"].iloc[0]
        c7 = g.loc[g["cutoff"] == 7, "median_ms"].iloc[0]
        c10 = g.loc[g["cutoff"] == 10, "median_ms"].iloc[0]
        best = g.loc[g["median_ms"].idxmin()]
        print(
            f"- {dist:16s} | Pure={pure:7.2f}ms | C7={c7:7.2f}ms | C10={c10:7.2f}ms "
            f"| Best C={int(best['cutoff']):3d} ({best['median_ms']:7.2f}ms, +{best['speedup_vs_pure_pct']:5.2f}%)"
        )

    print("\n=== TOM TAT KET QUA EXP 2 (KIEU DU LIEU) ===")
    for dtype, g in df2.groupby("datatype", sort=False):
        best = g.loc[g["median_ms"].idxmin()]
        print(
            f"- {dtype:8s} | Best C={int(best['cutoff']):3d} "
            f"({best['median_ms']:7.2f}ms, +{best['speedup_vs_pure_pct']:5.2f}%)"
        )

    print("\n=== TOM TAT KET QUA EXP 3 (DOUBLING TEST - DO DOC LOG-LOG) ===")
    for cutoff, g in df3.groupby("cutoff", sort=False):
        g = g.sort_values("n")
        slope, _ = np.polyfit(np.log2(g["n"]), np.log2(g["median_ms"]), 1)
        avg_ratio = g["doubling_ratio"].dropna().mean()
        print(f"- CUTOFF={int(cutoff):3d} | T(N)/T(N/2) trung binh = {avg_ratio:.3f} | Do doc log-log b = {slope:.3f}")


def main():
    CHARTS_DIR.mkdir(parents=True, exist_ok=True)

    df1 = pd.read_csv(RESULTS_DIR / "exp1_cutoff_summary.csv")
    df2 = pd.read_csv(RESULTS_DIR / "exp2_datatype_summary.csv")
    df3 = pd.read_csv(RESULTS_DIR / "exp3_doubling_summary.csv")

    plot_exp1_random(df1)
    plot_exp1_distributions(df1)
    plot_exp2_datatypes(df2)
    plot_exp3_doubling(df3)
    print_console_summary(df1, df2, df3)

    print(f"\nDa xuat 4 bieu do PNG do phan giai cao tai: {CHARTS_DIR.resolve()}")


if __name__ == "__main__":
    main()