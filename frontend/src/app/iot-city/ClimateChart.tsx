/**
 * Climate Time Series Chart component using Recharts
 */

import { useMemo } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import type { ClimateReport } from './types';

interface ClimateChartProps {
  data: ClimateReport[];
}

export function ClimateChart({ data }: ClimateChartProps) {
  // Transform data for the chart - reverse to show oldest first (left to right)
  const chartData = useMemo(() => {
    return [...data].reverse().map((report) => ({
      time: new Date(report.timestampMillis).toLocaleTimeString('fr-FR', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      }),
      timestamp: report.timestampMillis,
      temperature: report.aggregatedData.TEMPERATURE ?? null,
      humidity: report.aggregatedData.HUMIDITY ?? null,
      pressure: report.aggregatedData.PRESSURE
        ? report.aggregatedData.PRESSURE / 10
        : null, // Scale pressure for display
    }));
  }, [data]);

  if (chartData.length === 0) {
    return (
      <div className="h-64 flex items-center justify-center text-muted-foreground">
        No data available
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={320}>
      <LineChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
        <XAxis
          dataKey="time"
          className="text-xs"
          tick={{ fill: 'hsl(var(--muted-foreground))' }}
          tickLine={{ stroke: 'hsl(var(--muted-foreground))' }}
        />
        <YAxis
          yAxisId="temp"
          orientation="left"
          domain={['auto', 'auto']}
          className="text-xs"
          tick={{ fill: 'hsl(var(--muted-foreground))' }}
          tickLine={{ stroke: 'hsl(var(--muted-foreground))' }}
          label={{
            value: 'Temp (C) / Humidity (%)',
            angle: -90,
            position: 'insideLeft',
            fill: 'hsl(var(--muted-foreground))',
            style: { fontSize: '10px' },
          }}
        />
        <YAxis
          yAxisId="pressure"
          orientation="right"
          domain={['auto', 'auto']}
          className="text-xs"
          tick={{ fill: 'hsl(var(--muted-foreground))' }}
          tickLine={{ stroke: 'hsl(var(--muted-foreground))' }}
          label={{
            value: 'Pressure (x10 hPa)',
            angle: 90,
            position: 'insideRight',
            fill: 'hsl(var(--muted-foreground))',
            style: { fontSize: '10px' },
          }}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: 'hsl(var(--card))',
            border: '1px solid hsl(var(--border))',
            borderRadius: '6px',
            color: 'hsl(var(--card-foreground))',
          }}
          formatter={(value: number, name: string) => {
            if (name === 'temperature') return [`${value?.toFixed(1)} C`, 'Temperature'];
            if (name === 'humidity') return [`${value?.toFixed(1)}%`, 'Humidity'];
            if (name === 'pressure') return [`${(value * 10)?.toFixed(0)} hPa`, 'Pressure'];
            return [value, name];
          }}
        />
        <Legend
          wrapperStyle={{ paddingTop: '10px' }}
          formatter={(value: string) => {
            if (value === 'temperature') return 'Temperature (C)';
            if (value === 'humidity') return 'Humidity (%)';
            if (value === 'pressure') return 'Pressure (hPa)';
            return value;
          }}
        />
        <Line
          yAxisId="temp"
          type="monotone"
          dataKey="temperature"
          stroke="#ef4444"
          strokeWidth={2}
          dot={{ fill: '#ef4444', r: 3 }}
          activeDot={{ r: 5 }}
          connectNulls
          name="temperature"
        />
        <Line
          yAxisId="temp"
          type="monotone"
          dataKey="humidity"
          stroke="#3b82f6"
          strokeWidth={2}
          dot={{ fill: '#3b82f6', r: 3 }}
          activeDot={{ r: 5 }}
          connectNulls
          name="humidity"
        />
        <Line
          yAxisId="pressure"
          type="monotone"
          dataKey="pressure"
          stroke="#22c55e"
          strokeWidth={2}
          dot={{ fill: '#22c55e', r: 3 }}
          activeDot={{ r: 5 }}
          connectNulls
          name="pressure"
        />
      </LineChart>
    </ResponsiveContainer>
  );
}
