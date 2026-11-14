config.devServer = {
  ...config.devServer, // Merge with other devServer settings
  "historyApiFallback": true,
  port: 9080,
  client: {
    overlay: false,
  },
};
