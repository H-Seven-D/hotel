<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([BarChart, LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const chartElement = ref()
let chart

function render() {
  if (!chartElement.value) return
  if (!chart) {
    chart = echarts.init(chartElement.value)
  }
  chart.setOption({
    animationDuration: 500,
    grid: { left: 16, right: 18, top: 42, bottom: 12, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#0f2e6d',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 }
    },
    legend: {
      top: 0,
      right: 0,
      textStyle: { color: '#667085', fontSize: 12 },
      itemWidth: 10,
      itemHeight: 10
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.data.map((item) => item.label),
      axisLine: { lineStyle: { color: '#e5eaf2' } },
      axisTick: { show: false },
      axisLabel: { color: '#7a8496', fontSize: 11, hideOverlap: true, interval: 1 }
    },
    yAxis: [
      {
        type: 'value',
        splitLine: { lineStyle: { color: '#eef2f7' } },
        axisLabel: { color: '#8b94a3', fontSize: 11 }
      },
      {
        type: 'value',
        splitLine: { show: false },
        axisLabel: { show: false }
      }
    ],
    series: [
      {
        name: '预订数',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: props.data.map((item) => item.count),
        lineStyle: { width: 3, color: '#2563eb' },
        itemStyle: { color: '#2563eb' },
        areaStyle: { color: 'rgba(37, 99, 235, 0.1)' }
      },
      {
        name: '金额',
        type: 'bar',
        yAxisIndex: 1,
        barWidth: 14,
        data: props.data.map((item) => item.revenue),
        itemStyle: { color: '#bfd3fa', borderRadius: [4, 4, 0, 0] }
      }
    ]
  })
}

function resize() {
  chart?.resize()
}

watch(() => props.data, () => nextTick(render), { deep: true })

onMounted(() => {
  render()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
})
</script>

<template>
  <div ref="chartElement" class="chart-box"></div>
</template>
