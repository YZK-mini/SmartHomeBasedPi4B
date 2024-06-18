from openai import OpenAI


def semanticAnalysis(text):
    client = OpenAI(
        base_url="https://integrate.api.nvidia.com/v1",
        api_key="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"
    )
    completion = client.chat.completions.create(
        model="meta/llama3-70b-instruct",
        messages=[{"role": "system",
                   "content": "你将作为家居控制终端直接给出设备名称(厨房灯、卧室灯、客厅灯、窗帘、温湿度传感器)和操作，并使用中文"},
                  {"role": "user", "content": text}],
        temperature=0.2,
        top_p=1,
        max_tokens=512,
        stream=True
    )
    text = ""
    for chunk in completion:
        content = chunk.choices[0].delta.content
        if content is not None:
            text += content
    print(text)
    return textParaphrasing(text), text


def textParaphrasing(text) -> list:
    movementList = []
    if "卧室灯" in text:
        if "开" in text:
            movementList.append([1, 1])
        elif "关" in text:
            movementList.append([1, 0])

    if "客厅灯" in text:
        if "开" in text:
            movementList.append([2, 1])
        elif "关" in text:
            movementList.append([2, 0])

    if "厨房灯" in text:
        if "开" in text:
            movementList.append([3, 1])
        elif "关" in text:
            movementList.append([3, 0])

    if "窗帘" in text:
        if "开" in text:
            movementList.append([4, 1])
        elif "关" in text:
            movementList.append([4, 0])

    if "温湿度" in text or "天气" in text:
        movementList.append([5, 1])
    return movementList


if __name__ == "__main__":
    print(semanticAnalysis("你好"))
