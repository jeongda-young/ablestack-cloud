import en from '../../../public/locales/en.json'
import ko from '../../../public/locales/ko_KR.json'

const expectedLabels = {
  'label.dr.operation.progress': {
    en: 'Overall operation progress',
    ko: '전체 작업 진행률'
  },
  'label.dr.transfer.progress': {
    en: 'Data transfer progress',
    ko: '데이터 전송 진행률'
  },
  'label.dr.cbt.pending.activation': {
    en: 'CBT configured, activation pending',
    ko: 'CBT 설정 완료, 활성 검증 대기'
  }
}

describe('DR locale labels', () => {
  for (const [key, expected] of Object.entries(expectedLabels)) {
    it(`defines ${key} in English and Korean`, () => {
      expect(en[key]).toBe(expected.en)
      expect(ko[key]).toBe(expected.ko)
    })
  }
})
