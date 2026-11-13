// A bar chart drawn with plain SVG (no chart library): one bar per day, its count above it, the date below.
// SVG draws in its own coordinates (the viewBox, here 1200 × 200: wide and low, like the page it sits in); the
// browser scales it to the card's width.

const WIDTH = 1200;
const HEIGHT = 200;
const TOP = 20;      // room for the count above the tallest bar
const BOTTOM = 30;   // room for the date labels
const CHART_HEIGHT = HEIGHT - TOP - BOTTOM;

// "2026-11-01" (a UTC day) → "Nov 1" in the user's language.
function dayLabel(date: string): string {
  const day = new Date(`${date}T00:00:00Z`);
  return day.toLocaleDateString(undefined, { month: 'short', day: 'numeric', timeZone: 'UTC' });
}

export default function BarChart({ days }: { days: { date: string; count: number }[] }) {
  const max = Math.max(1, ...days.map((d) => d.count)); // at least 1, so an all-zero chart doesn't divide by 0
  const slot = WIDTH / days.length; // the horizontal space each day gets
  const barWidth = Math.min(slot * 0.5, 36);

  return (
    <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} className="bar-chart" role="img"
         aria-label={`Alerts per day: ${days.map((d) => `${d.date} ${d.count}`).join(', ')}`}>
      {/* the base line */}
      <line x1={0} y1={TOP + CHART_HEIGHT} x2={WIDTH} y2={TOP + CHART_HEIGHT} className="bar-chart-axis" />
      {days.map((day, i) => {
        const barHeight = (day.count / max) * CHART_HEIGHT;
        const x = i * slot + (slot - barWidth) / 2; // centred in its slot
        const y = TOP + CHART_HEIGHT - barHeight;   // SVG's y grows downwards, so bars start at the bottom
        const centre = i * slot + slot / 2;
        return (
          <g key={day.date}>
            <rect x={x} y={y} width={barWidth} height={barHeight} rx={3} className="bar-chart-bar" />
            {/* No "0" over empty days: the base line already says it, and the chart stays calm. */}
            {day.count > 0 && (
              <text x={centre} y={y - 6} textAnchor="middle" className="bar-chart-count">{day.count}</text>
            )}
            <text x={centre} y={HEIGHT - 10} textAnchor="middle" className="bar-chart-label">{dayLabel(day.date)}</text>
          </g>
        );
      })}
    </svg>
  );
}
