<template>

  <a-modal
    title="修改头像"
    :visible="visible"
    :maskClosable="false"
    :confirmLoading="confirmLoading"
    :width="800"
    :footer="null"
    @cancel="cancelHandel">
    <a-row>
      <a-col :xs="24" :md="12" :style="{height: '350px'}">
        <vue-cropper
          ref="cropper"
          :img="options.img"
          :info="true"
          :autoCrop="options.autoCrop"
          :autoCropWidth="options.autoCropWidth"
          :autoCropHeight="options.autoCropHeight"
          :fixedBox="options.fixedBox"
          @realTime="realTime"
        >
        </vue-cropper>
      </a-col>
      <a-col :xs="24" :md="12" :style="{height: '350px'}">
        <div class="avatar-upload-preview">
          <img :src="previews.url" :style="previews.img"/>
        </div>
      </a-col>
    </a-row>
    <br>
    <a-row>
      <a-col :sm="2" :xs="2">
        <a-upload name="file" :beforeUpload="beforeUpload" :showUploadList="false" accept=".png,.jpg,.jpeg,.gif">
          <a-button icon="upload">选择图片</a-button>
        </a-upload>
      </a-col>
      <a-col :sm="{span: 1, offset: 2}" :xs="2">
        <a-button icon="plus" @click="changeScale(1)"/>
      </a-col>
      <a-col :sm="{span: 1, offset: 1}" :xs="2">
        <a-button icon="minus" @click="changeScale(-1)"/>
      </a-col>
      <a-col :sm="{span: 1, offset: 1}" :xs="2">
        <a-button icon="undo" @click="rotateLeft"/>
      </a-col>
      <a-col :sm="{span: 1, offset: 1}" :xs="2">
        <a-button icon="redo" @click="rotateRight"/>
      </a-col>
      <a-col :sm="{span: 2, offset: 6}" :xs="2">
        <a-button type="primary" @click="finish('blob')">保存</a-button>
      </a-col>
    </a-row>
  </a-modal>

</template>
<script>
import api from '@/services/api'
import axios from 'axios'
export default {
  data () {
    return {
      visible: false,
      id: null,
      confirmLoading: false,
      fileList: [],
      uploading: false,
      options: {
        // img: 'https://zos.alipayobjects.com/rmsportal/jkjgkEfvpUPVyRjUImniVslZfWPnJuuZ.png',
        img: '',
        autoCrop: true,
        autoCropWidth: 200,
        autoCropHeight: 200,
        fixedBox: true
      },
      previews: {}
    }
  },
  methods: {
    edit (id) {
      this.visible = true
      this.id = id
      /* 获取原始头像 */
    },
    close () {
      this.id = null
      this.visible = false
    },
    cancelHandel () {
      this.close()
    },
    changeScale (num) {
      num = num || 1
      this.$refs.cropper.changeScale(num)
    },
    rotateLeft () {
      this.$refs.cropper.rotateLeft()
    },
    rotateRight () {
      this.$refs.cropper.rotateRight()
    },
    beforeUpload (file) {
      const reader = new FileReader()
      // 把Array Buffer转化为blob 如果是base64不需要
      // 转化为base64
      reader.readAsDataURL(file)
      reader.onload = () => {
        this.options.img = reader.result
      }
      // 转化为blob
      // reader.readAsArrayBuffer(file)

      return false
    },

    // 上传图片（点击上传按钮）
    finish (type) {
      const _this = this
      const formData = new FormData()
      // 输出
      if (type === 'blob') {
        this.$refs.cropper.getCropBlob((data) => {
          const img = window.URL.createObjectURL(data)
          this.model = true
          this.modelSrc = img
          const fileName = `${this.moment().format('YYYYMMDD')}_avatar.png`
          formData.append('file', data, fileName)
          // let request = new XMLHttpRequest()
          // request.open('POST', api.uploadAvatar)
          // request.send(formData)
          axios
            .post(api.uploadAvatar, formData).then(response => {
              let res = response.data
              if (res.code === 200) {
                _this.imgFile = ''
                _this.headImg = res.data.url // 完整路径
                _this.uploadImgRelaPath = res.data.url // 非完整路径
                _this.$message.success('上传成功')
                this.visible = false
                _this.$emit('ok', res.data.url)
              } else {
                this.$message.error(res.message)
              }
            }).catch(() => {
              this.showError();
            })
        })
      } else {
        this.$refs.cropper.getCropData((data) => {
          this.model = true
          this.modelSrc = data
        })
      }
    },
    okHandel () {
      const vm = this

      vm.confirmLoading = true
      setTimeout(() => {
        vm.confirmLoading = false
        vm.close()
        vm.$message.success('上传头像成功')
      }, 2000)
    },

    realTime (data) {
      this.previews = data
    }
  }
}
</script>

<style lang="scss" scoped>

  .avatar-upload-preview {
    position: absolute;
    top: 50%;
    transform: translate(50%, -50%);
    width: 200px;
    height: 200px;
    border-radius: 50%;
    box-shadow: 0 0 4px #ccc;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
    }
  }
</style>
