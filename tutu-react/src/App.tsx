import { useState } from 'react'

function App() {
  const [count, setCount] = useState(0)

  return (
    <main>
      <h1>React 学习页面</h1>

      <section>
        <h2>计数器</h2>
        <p>当前数量：{count}</p>

        <button onClick={() => setCount(count + 1)}>
          加 1
        </button>

        <button onClick={() => setCount(count - 1)}>
          减 1
        </button>

        <button onClick={() => setCount(0)}>
          重置
        </button>
      </section>
    </main>
  )
}

export default App